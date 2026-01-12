#include <algorithm>
#include <chrono>
#include <iostream>
#include <optional>
#include <random>
#include <string>
#include <thread>
#include <vector>

#ifdef USE_MPI
#include <mpi.h>
#endif

#include "AppSettings.h"
#include "Blockchain.h"
#include "MiningService.h"
#include "PeerNetwork.h"
#include "DistributedMining.h"

int parseThreadsArgument(int argc, char* argv[]) {
    unsigned int hwThreads = std::thread::hardware_concurrency();
    int numThreads = (hwThreads > 0) ? static_cast<int>(hwThreads) : 1;
    
    for (int i = 1; i < argc; ++i) {
        std::string arg = argv[i];
        if (arg == "--threads" && i + 1 < argc) {
            try {
                int threads = std::stoi(argv[i + 1]);
                if (threads < 1) {
                    std::cerr << "Warning: Invalid thread count " << threads 
                              << ", using default: " << numThreads << "\n";
                } else {
                    numThreads = threads;
                }
            } catch (const std::exception& e) {
                std::cerr << "Warning: Failed to parse thread count, using default: " 
                          << numThreads << "\n";
            }
            break;
        }
    }
    
    return numThreads;
}

bool parseVerboseFlag(int argc, char* argv[]) {
    for (int i = 1; i < argc; ++i) {
        std::string arg = argv[i];
        if (arg == "--verbose" || arg == "--debug-mpi") {
            return true;
        }
    }
    return false;
}

int main(int argc, char* argv[])
{
#ifdef USE_MPI
    MPI_Init(&argc, &argv);
    
    int rank, size;
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    MPI_Comm_size(MPI_COMM_WORLD, &size);
    
    bool useMPI = (size > 1);
    
    if (rank == 0) {
        std::cout << "MPI initialized: " << size << " process(es)\n";
    }
#else
    int rank = 0;
    int size = 1;
    bool useMPI = false;
#endif

    int numThreads = parseThreadsArgument(argc, argv);
    bool verbose = parseVerboseFlag(argc, argv);
    
    if (rank == 0) {
        std::cout << "Mining threads per process: " << numThreads << "\n";
        if (verbose) {
            std::cout << "Verbose logging enabled\n";
        }
        std::cout << "Enter your name: ";
    }
    
    std::string username;
    if (rank == 0) {
        std::getline(std::cin, username);
    }
    
#ifdef USE_MPI
    if (size > 1) {
        int usernameLen = static_cast<int>(username.size());
        MPI_Bcast(&usernameLen, 1, MPI_INT, 0, MPI_COMM_WORLD);
        if (rank != 0) {
            username.resize(usernameLen);
        }
        MPI_Bcast(const_cast<char*>(username.data()), usernameLen, MPI_CHAR, 0, MPI_COMM_WORLD);
    }
#endif
    
    if (username.empty())
    {
        if (rank == 0) {
            std::cout << "Name is required. Exiting.\n";
        }
#ifdef USE_MPI
        MPI_Finalize();
#endif
        return 0;
    }

    std::mt19937 rng(std::random_device{}());
    std::uniform_int_distribution<int> dist(AppSettings::MinPort,
                                            AppSettings::MaxPort);
    int port = dist(rng);

    Blockchain blockchain(AppSettings::DefaultDifficulty,
                          AppSettings::BlockGenerationInterval,
                          AppSettings::DifficultyAdjustmentInterval);
    MiningService miningService(blockchain, numThreads, useMPI);
    PeerNetwork network;

    network.SetMessageHandler([rank](const std::string &msg)
                              { 
                                  if (rank == 0) {
                                      std::cout << msg << "\n" << std::flush;
                                  }
                              });

    network.SetChainHandler([&, rank, useMPI, size, verbose](const std::vector<Block> &chain)
                            {
        if (rank != 0) return;
        
        auto localSize = blockchain.GetChain().size();
        if (chain.size() <= 1 && localSize <= 1) {
            std::cout << "No chain exchanged, peers have no chain.\n" << std::flush;
            return;
        }

        if (chain.size() <= localSize) {
            if (chain.size() == localSize) {
                std::cout << "[PEER] Didn't update chain, same length (" << localSize << ").\n" << std::flush;
            } else {
                std::cout << "[PEER] Block rejected from peer (local: " << localSize 
                          << ", received: " << chain.size() << " - shorter).\n" << std::flush;
            }
            return;
        }

        if (blockchain.ReplaceChain(chain)) {
            std::cout << "[PEER] Block accepted from peer - chain updated (new length: " << chain.size() << ").\n" << std::flush;
            
#ifdef USE_MPI
            if (useMPI && size > 1) {
                DistributedMining::broadcastChainToAllRanksMPI(blockchain.GetChain(), verbose);
            }
#endif
        } else {
            std::cout << "[PEER] Block rejected from peer (local: " << localSize 
                      << ", received: " << chain.size() << " - validation failed).\n" << std::flush;
        } });

    miningService.SetBlockMinedHandler([rank](const Block &block)
                                       { 
                                           if (rank == 0) {
                                               std::cout << "Block " << block.index << " mined successfully\n" << std::flush;
                                           }
                                       });

    if (!network.StartServer(port))
    {
        if (rank == 0) {
            std::cout << "Failed to start server.\n";
        }
        return 1;
    }
    
    if (rank == 0) {
        std::cout << "Server port: " << port << "\n";
        std::cout << "Commands: connect <port>, mine, show, exit\n";
    }
    std::string command;
    while (true)
    {
#ifdef USE_MPI
        if (useMPI && size > 1 && rank != 0) {
            auto chainUpdate = DistributedMining::checkForChainUpdateMPI(verbose);
            if (!chainUpdate.empty()) {
                auto localSize = blockchain.GetChain().size();
                if (blockchain.ReplaceChain(chainUpdate)) {
                    if (verbose) {
                        std::cout << "[rank " << rank << "/" << size << "] Updated chain from rank 0 (size=" 
                                  << chainUpdate.size() << ")\n" << std::flush;
                    }
                } else if (verbose && chainUpdate.size() <= localSize) {
                    std::cout << "[rank " << rank << "/" << size << "] Chain update rejected (local: " << localSize 
                              << ", received: " << chainUpdate.size() << " - not better)\n" << std::flush;
                }
            }
        }
        
        if (useMPI && size > 1) {
            if (rank == 0) {
                std::cout << "> ";
                if (!std::getline(std::cin, command))
                {
                    command = "exit";
                }
                int cmdLen = static_cast<int>(command.size());
                MPI_Bcast(&cmdLen, 1, MPI_INT, 0, MPI_COMM_WORLD);
                MPI_Bcast(const_cast<char*>(command.data()), cmdLen, MPI_CHAR, 0, MPI_COMM_WORLD);
            } else {
                int cmdLen;
                MPI_Bcast(&cmdLen, 1, MPI_INT, 0, MPI_COMM_WORLD);
                command.resize(cmdLen);
                MPI_Bcast(const_cast<char*>(command.data()), cmdLen, MPI_CHAR, 0, MPI_COMM_WORLD);
            }
        } else {
            std::cout << "> ";
            if (!std::getline(std::cin, command))
            {
                break;
            }
        }
#else
        std::cout << "> ";
        if (!std::getline(std::cin, command))
        {
            break;
        }
#endif
        if (command == "exit")
        {
            break;
        }
        else if (command.rfind("connect", 0) == 0)
        {
            auto pos = command.find(' ');
            if (pos == std::string::npos)
            {
                if (rank == 0) {
                    std::cout << "Usage: connect <port>\n";
                }
                continue;
            }
            int peerPort = std::stoi(command.substr(pos + 1));
            if (peerPort < 1 || peerPort > 65535)
            {
                if (rank == 0) {
                    std::cout << "Invalid port.\n";
                }
                continue;
            }
            
#ifdef USE_MPI
            if (useMPI && size > 1) {
                if (rank == 0) {
                    if (network.ConnectToPeer(peerPort))
                    {
                        auto chain = blockchain.GetChain();
                        if (chain.size() > 1)
                        {
                            network.BroadcastChain(chain);
                        }
                        else
                        {
                            if (rank == 0) {
                                std::cout << "No chain to broadcast yet.\n";
                            }
                        }
                    }
                }
            } else {
#endif
                if (network.ConnectToPeer(peerPort))
                {
                    auto chain = blockchain.GetChain();
                    if (chain.size() > 1)
                    {
                        network.BroadcastChain(chain);
                    }
                    else
                    {
                        if (rank == 0) {
                            std::cout << "No chain to broadcast yet.\n";
                        }
                    }
                }
#ifdef USE_MPI
            }
#endif
        }
        else if (command == "mine")
        {
#ifdef USE_MPI
            if (useMPI && size > 1 && rank != 0) {
                auto chainUpdate = DistributedMining::checkForChainUpdateMPI(verbose);
                if (!chainUpdate.empty()) {
                    auto localSize = blockchain.GetChain().size();
                    if (blockchain.ReplaceChain(chainUpdate)) {
                        if (verbose) {
                            std::cout << "[rank " << rank << "/" << size << "] Updated chain from rank 0 (size=" 
                                      << chainUpdate.size() << ")\n" << std::flush;
                        }
                    } else if (verbose && chainUpdate.size() <= localSize) {
                        std::cout << "[rank " << rank << "/" << size << "] Chain update rejected (local: " << localSize 
                                  << ", received: " << chainUpdate.size() << " - not better)\n" << std::flush;
                    }
                }
            }
#endif
            auto mined = miningService.MineBlock(AppSettings::DefaultBlockData, verbose);
            if (mined)
            {
#ifdef USE_MPI
                if (useMPI && size > 1) {
                    if (rank == 0) {
                        std::cout << "Block mined and broadcasted to peers.\n" << std::flush;
                        network.BroadcastChain(blockchain.GetChain());
                    }
                } else {
                    std::cout << "Block mined and broadcasted.\n" << std::flush;
                    network.BroadcastChain(blockchain.GetChain());
                }
#else
                std::cout << "Block mined and broadcasted.\n" << std::flush;
                network.BroadcastChain(blockchain.GetChain());
#endif
            }
            else
            {
                if (verbose) {
                    std::cout << "[rank " << rank << "/" << size << "] Mining failed.\n" << std::flush;
                }
            }
        }
        else if (command == "show")
        {
            if (rank == 0) {
                auto chain = blockchain.GetChain();
                std::cout << "Chain length: " << chain.size() << "\n";
                if (!chain.empty())
                {
                    const auto &last = chain.back();
                    std::cout << "Last block index: " << last.index << "\n";
                    std::cout << "Hash: " << last.hash << "\n";
                }
                std::cout << std::flush;
            }
        }
        else
        {
            if (rank == 0) {
                std::cout << "Unknown command.\n";
            }
        }

    }

    network.Stop();
    
#ifdef USE_MPI
    MPI_Finalize();
#endif
    
    return 0;
}
