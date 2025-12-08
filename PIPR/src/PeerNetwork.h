#pragma once

#ifdef _WIN32
#include <winsock2.h>
#include <ws2tcpip.h>
#pragma comment(lib, "Ws2_32.lib")
#endif

#include <atomic>
#include <functional>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

#include "Block.h"

class PeerNetwork {
public:
    using MessageHandler = std::function<void(const std::string&)>;
    using ChainHandler = std::function<void(const std::vector<Block>&)>;

    PeerNetwork();
    ~PeerNetwork();

    bool StartServer(int port);
    bool ConnectToPeer(int port);
    bool BroadcastMessage(const std::string& message);
    bool BroadcastChain(const std::vector<Block>& chain);
    void Stop();

    void SetMessageHandler(MessageHandler handler);
    void SetChainHandler(ChainHandler handler);

    int GetServerPort() const { return serverPort_; }

private:
    void ListenLoop();
    void HandleClient(SOCKET client);
    bool PeerExists(int port);
    void AddPeer(SOCKET peer);
    void RemovePeer(SOCKET peer);

    std::vector<SOCKET> peers_;
    std::mutex peersMutex_;
    std::thread serverThread_;
    std::atomic_bool running_{false};
    SOCKET serverSocket_{INVALID_SOCKET};
    int serverPort_{0};

    MessageHandler messageHandler_;
    ChainHandler chainHandler_;
};

