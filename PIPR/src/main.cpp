#include <chrono>
#include <iostream>
#include <optional>
#include <random>
#include <string>

#include "AppSettings.h"
#include "Blockchain.h"
#include "MiningService.h"
#include "PeerNetwork.h"

int main()
{
    std::cout << "Enter your name: ";
    std::string username;
    std::getline(std::cin, username);
    if (username.empty())
    {
        std::cout << "Name is required. Exiting.\n";
        return 0;
    }

    std::mt19937 rng(std::random_device{}());
    std::uniform_int_distribution<int> dist(AppSettings::MinPort,
                                            AppSettings::MaxPort);
    int port = dist(rng);

    Blockchain blockchain(AppSettings::DefaultDifficulty,
                          AppSettings::BlockGenerationInterval,
                          AppSettings::DifficultyAdjustmentInterval);
    MiningService miningService(blockchain);
    PeerNetwork network;

    network.SetMessageHandler([](const std::string &msg)
                              { std::cout << msg << "\n"; });

    network.SetChainHandler([&](const std::vector<Block> &chain)
                            {
        auto localSize = blockchain.GetChain().size();
        if (chain.size() <= 1 && localSize <= 1) {
            std::cout << "No chain exchanged, peers have no chain.\n";
            return;
        }

        if (blockchain.ReplaceChain(chain)) {
            std::cout << "Replaced chain.\n";
        } else {
            std::cout << "Received chain rejected.\n";
        } });

    miningService.SetBlockMinedHandler([](const Block &block)
                                       { std::cout << "Block " << block.index << " mined successfully\n"; });

    if (!network.StartServer(port))
    {
        std::cout << "Failed to start server.\n";
        return 1;
    }
    std::cout << "Server port: " << port << "\n";

    std::cout << "Commands: connect <port>, mine, show, exit\n";
    std::string command;
    while (true)
    {
        std::cout << "> ";
        if (!std::getline(std::cin, command))
        {
            break;
        }
        if (command == "exit")
        {
            break;
        }
        else if (command.rfind("connect", 0) == 0)
        {
            auto pos = command.find(' ');
            if (pos == std::string::npos)
            {
                std::cout << "Usage: connect <port>\n";
                continue;
            }
            int peerPort = std::stoi(command.substr(pos + 1));
            if (peerPort < 1 || peerPort > 65535)
            {
                std::cout << "Invalid port.\n";
                continue;
            }
            if (network.ConnectToPeer(peerPort))
            {
                auto chain = blockchain.GetChain();
                if (chain.size() > 1)
                {
                    network.BroadcastChain(chain);
                }
                else
                {
                    std::cout << "No chain to broadcast yet.\n";
                }
            }
        }
        else if (command == "mine")
        {
            auto mined = miningService.MineBlock(AppSettings::DefaultBlockData);
            if (mined)
            {
                std::cout << "Block mined and broadcasted.\n";
                network.BroadcastChain(blockchain.GetChain());
            }
            else
            {
                std::cout << "Mining failed.\n";
            }
        }
        else if (command == "show")
        {
            auto chain = blockchain.GetChain();
            std::cout << "Chain length: " << chain.size() << "\n";
            if (!chain.empty())
            {
                const auto &last = chain.back();
                std::cout << "Last block index: " << last.index << "\n";
                std::cout << "Hash: " << last.hash << "\n";
            }
        }
        else
        {
            std::cout << "Unknown command.\n";
        }
    }

    network.Stop();
    return 0;
}
