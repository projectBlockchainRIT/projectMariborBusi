#include "PeerNetwork.h"

#include <algorithm>
#include <chrono>
#include <iostream>
#include <optional>
#include <string>
#include <thread>

#include "JsonUtils.h"

PeerNetwork::PeerNetwork() {
#ifdef _WIN32
    WSADATA wsaData;
    WSAStartup(MAKEWORD(2, 2), &wsaData);
#endif
}

PeerNetwork::~PeerNetwork() {
    Stop();
#ifdef _WIN32
    WSACleanup();
#endif
}

bool PeerNetwork::StartServer(int port) {
    serverPort_ = port;
#ifdef _WIN32
    serverSocket_ = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (serverSocket_ == INVALID_SOCKET) {
        return false;
    }

    sockaddr_in service{};
    service.sin_family = AF_INET;
    service.sin_addr.s_addr = inet_addr("127.0.0.1");
    service.sin_port = htons(static_cast<u_short>(serverPort_));

    if (bind(serverSocket_, reinterpret_cast<SOCKADDR*>(&service),
             sizeof(service)) == SOCKET_ERROR) {
        closesocket(serverSocket_);
        serverSocket_ = INVALID_SOCKET;
        return false;
    }

    if (listen(serverSocket_, SOMAXCONN) == SOCKET_ERROR) {
        closesocket(serverSocket_);
        serverSocket_ = INVALID_SOCKET;
        return false;
    }
#endif
    running_ = true;
    serverThread_ = std::thread(&PeerNetwork::ListenLoop, this);
    if (messageHandler_) {
        messageHandler_("Server started on port " + std::to_string(serverPort_));
    }
    return true;
}

void PeerNetwork::ListenLoop() {
#ifdef _WIN32
    while (running_) {
        SOCKET clientSocket = accept(serverSocket_, nullptr, nullptr);
        if (clientSocket == INVALID_SOCKET) {
            if (!running_) break;
            continue;
        }
        AddPeer(clientSocket);
        if (messageHandler_) {
            messageHandler_("New peer connected");
        }
        std::thread(&PeerNetwork::HandleClient, this, clientSocket).detach();
    }
#endif
}

void PeerNetwork::HandleClient(SOCKET client) {
#ifdef _WIN32
    constexpr int BufferSize = 1 << 20;
    std::string message;
    std::vector<char> buffer(BufferSize);
    while (running_) {
        int bytesRead = recv(client, buffer.data(), BufferSize, 0);
        if (bytesRead <= 0) break;
        message.assign(buffer.begin(), buffer.begin() + bytesRead);

        if (!message.empty() && message.front() == '[' &&
            message.back() == ']') {
            auto chainOpt = DeserializeChain(message);
            if (chainOpt && chainHandler_) {
                chainHandler_(*chainOpt);
                const auto& chain = *chainOpt;
                const auto& last = chain.back();
                if (messageHandler_) {
                    messageHandler_("Index: " + std::to_string(last.index));
                    messageHandler_("Prev. Hash: " + last.previousHash);
                    messageHandler_("Hash: " + last.hash);
                    messageHandler_("Difficulty: " +
                                    std::to_string(last.difficulty));
                }
            } else if (messageHandler_) {
                messageHandler_("Failed to deserialize chain");
            }
        } else if (messageHandler_) {
            messageHandler_("Received: " + message);
        }
    }
    closesocket(client);
    RemovePeer(client);
#endif
}

bool PeerNetwork::ConnectToPeer(int port) {
    if (port == serverPort_) {
        if (messageHandler_) {
            messageHandler_("Cannot connect to your own server port.");
        }
        return false;
    }

    if (PeerExists(port)) {
        if (messageHandler_) {
            messageHandler_("Already connected to port.");
        }
        return false;
    }

#ifdef _WIN32
    SOCKET connectSocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (connectSocket == INVALID_SOCKET) {
        return false;
    }

    sockaddr_in clientService{};
    clientService.sin_family = AF_INET;
    inet_pton(AF_INET, "127.0.0.1", &clientService.sin_addr);
    clientService.sin_port = htons(static_cast<u_short>(port));

    if (connect(connectSocket, reinterpret_cast<SOCKADDR*>(&clientService),
                sizeof(clientService)) == SOCKET_ERROR) {
        closesocket(connectSocket);
        if (messageHandler_) {
            messageHandler_("Error connecting to peer.");
        }
        return false;
    }

    AddPeer(connectSocket);
    if (messageHandler_) {
        messageHandler_("Connected to peer at " + std::to_string(port));
    }
#endif
    return true;
}

bool PeerNetwork::BroadcastMessage(const std::string& message) {
#ifdef _WIN32
    auto data = message;
    std::vector<SOCKET> peersCopy;
    {
        std::scoped_lock lock(peersMutex_);
        peersCopy = peers_;
    }
    for (auto peer : peersCopy) {
        send(peer, data.c_str(), static_cast<int>(data.size()), 0);
    }
#endif
    return true;
}

bool PeerNetwork::BroadcastChain(const std::vector<Block>& chain) {
    auto json = SerializeChain(chain);
#ifdef _WIN32
    std::vector<SOCKET> peersCopy;
    {
        std::scoped_lock lock(peersMutex_);
        peersCopy = peers_;
    }
    for (auto peer : peersCopy) {
        send(peer, json.c_str(), static_cast<int>(json.size()), 0);
    }
#endif
    return true;
}

void PeerNetwork::SetMessageHandler(MessageHandler handler) {
    messageHandler_ = std::move(handler);
}

void PeerNetwork::SetChainHandler(ChainHandler handler) {
    chainHandler_ = std::move(handler);
}

void PeerNetwork::Stop() {
    running_ = false;
#ifdef _WIN32
    if (serverSocket_ != INVALID_SOCKET) {
        closesocket(serverSocket_);
        serverSocket_ = INVALID_SOCKET;
    }
#endif
    if (serverThread_.joinable()) {
        serverThread_.join();
    }

    std::vector<SOCKET> peersCopy;
    {
        std::scoped_lock lock(peersMutex_);
        peersCopy = peers_;
        peers_.clear();
    }
#ifdef _WIN32
    for (auto peer : peersCopy) {
        closesocket(peer);
    }
#endif
}

bool PeerNetwork::PeerExists(int port) {
    std::scoped_lock lock(peersMutex_);
#ifdef _WIN32
    for (auto peer : peers_) {
        sockaddr_in addr{};
        int addrLen = sizeof(addr);
        if (getpeername(peer, reinterpret_cast<sockaddr*>(&addr), &addrLen) ==
            0) {
            int peerPort = ntohs(addr.sin_port);
            if (peerPort == port) {
                return true;
            }
        }
    }
#endif
    return false;
}

void PeerNetwork::AddPeer(SOCKET peer) {
    std::scoped_lock lock(peersMutex_);
    peers_.push_back(peer);
}

void PeerNetwork::RemovePeer(SOCKET peer) {
    std::scoped_lock lock(peersMutex_);
    peers_.erase(std::remove(peers_.begin(), peers_.end(), peer), peers_.end());
}

