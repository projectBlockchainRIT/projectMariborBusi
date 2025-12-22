#include "DistributedMining.h"

#include <atomic>
#include <chrono>
#include <mutex>
#include <sstream>
#include <string>
#include <thread>
#include <vector>

#ifdef USE_MPI
#include <mpi.h>
#endif

#include "Block.h"
#include "Crypto.h"

namespace DistributedMining {
    namespace {
        bool hasLeadingZeros(const std::string& hash, int difficulty) {
            if (static_cast<int>(hash.size()) < difficulty) {
                return false;
            }
            for (int i = 0; i < difficulty; ++i) {
                if (hash[i] != '0') {
                    return false;
                }
            }
            return true;
        }

        // Serialize block to string for MPI communication
        std::string serializeBlock(const Block& block) {
            std::ostringstream oss;
            oss << block.index << "|"
                << block.data << "|"
                << block.TimestampString() << "|"
                << block.hash << "|"
                << block.previousHash << "|"
                << block.difficulty << "|"
                << block.nonce;
            return oss.str();
        }

        // Deserialize block from string
        Block deserializeBlock(const std::string& serialized) {
            std::istringstream iss(serialized);
            std::string token;
            std::vector<std::string> tokens;
            
            while (std::getline(iss, token, '|')) {
                tokens.push_back(token);
            }
            
            if (tokens.size() != 7) {
                // Return invalid block if deserialization fails
                return Block(0, "", std::chrono::system_clock::now(), "", 0, 0, false);
            }
            
            int index = std::stoi(tokens[0]);
            std::string data = tokens[1];
            auto timestamp = Block::ParseTimestamp(tokens[2]);
            std::string hash = tokens[3];
            std::string prevHash = tokens[4];
            int difficulty = std::stoi(tokens[5]);
            int nonce = std::stoi(tokens[6]);
            
            Block block(index, data, timestamp, prevHash, difficulty, nonce, false);
            block.hash = hash;
            return block;
        }
    }

#ifdef USE_MPI
    Block mineBlockMPI(
        int index,
        const std::string& data,
        const std::string& prevHash,
        int difficulty,
        int threadsPerProcess) {
        
        int rank, size;
        MPI_Comm_rank(MPI_COMM_WORLD, &rank);
        MPI_Comm_size(MPI_COMM_WORLD, &size);
        
        // If only one process, fall back to threaded mining
        if (size == 1) {
            // Use regular threaded mining (import from Mining namespace)
            // For now, we'll implement a simple version here
            if (threadsPerProcess <= 1) {
                // Sequential mining
                auto timestamp = std::chrono::system_clock::now();
                int nonce = 0;
                std::string hash;
                
                while (true) {
                    Block candidate(index, data, timestamp, prevHash, difficulty, nonce, false);
                    hash = candidate.computeHash();
                    
                    if (hasLeadingZeros(hash, difficulty)) {
                        candidate.hash = hash;
                        return candidate;
                    }
                    
                    ++nonce;
                }
            } else {
                // Multi-threaded mining (single process)
                auto timestamp = std::chrono::system_clock::now();
                std::atomic<bool> found(false);
                std::mutex resultMutex;
                Block result(index, data, timestamp, prevHash, difficulty, 0, false);
                bool resultSet = false;
                
                auto worker = [&](int threadId, int stride) {
                    int startNonce = threadId;
                    int nonce = startNonce;
                    
                    while (!found.load(std::memory_order_relaxed)) {
                        Block candidate(index, data, timestamp, prevHash, difficulty, nonce, false);
                        std::string hash = candidate.computeHash();
                        
                        if (hasLeadingZeros(hash, difficulty)) {
                            bool expected = false;
                            if (found.compare_exchange_strong(expected, true, std::memory_order_release, std::memory_order_relaxed)) {
                                std::lock_guard<std::mutex> lock(resultMutex);
                                if (!resultSet) {
                                    candidate.hash = hash;
                                    result = candidate;
                                    resultSet = true;
                                }
                            }
                            break;
                        }
                        
                        nonce += stride;
                        if (nonce < startNonce) {
                            break;
                        }
                    }
                };
                
                std::vector<std::thread> threads;
                threads.reserve(threadsPerProcess);
                
                for (int i = 0; i < threadsPerProcess; ++i) {
                    threads.emplace_back(worker, i, threadsPerProcess);
                }
                
                for (auto& t : threads) {
                    t.join();
                }
                
                if (!resultSet) {
                    // Fallback
                    int nonce = 0;
                    while (true) {
                        Block candidate(index, data, timestamp, prevHash, difficulty, nonce, false);
                        std::string hash = candidate.computeHash();
                        if (hasLeadingZeros(hash, difficulty)) {
                            candidate.hash = hash;
                            return candidate;
                        }
                        ++nonce;
                    }
                }
                
                return result;
            }
        }
        
        // Multi-process MPI mining
        auto timestamp = std::chrono::system_clock::now();
        
        // Calculate nonce space division
        // Total stride = numProcesses * threadsPerProcess
        int totalStride = size * threadsPerProcess;
        // Each process starts at rank * threadsPerProcess
        int processOffset = rank * threadsPerProcess;
        
        // Shared state for coordination
        std::atomic<bool> found(false);
        std::atomic<bool> stopMining(false);
        std::mutex resultMutex;
        Block result(index, data, timestamp, prevHash, difficulty, 0, false);
        bool resultSet = false;
        
        // MPI tags
        const int TAG_FOUND = 1;
        const int TAG_BLOCK_DATA = 2;
        
        // Communication thread to handle MPI messages (non-blocking)
        std::thread commThread([&]() {
            while (!stopMining.load(std::memory_order_relaxed)) {
                int flag = 0;
                MPI_Status status;
                
                // Check for found signal
                MPI_Iprobe(MPI_ANY_SOURCE, TAG_FOUND, MPI_COMM_WORLD, &flag, &status);
                if (flag) {
                    int foundSignal;
                    MPI_Recv(&foundSignal, 1, MPI_INT, status.MPI_SOURCE, TAG_FOUND, 
                            MPI_COMM_WORLD, &status);
                    stopMining.store(true, std::memory_order_release);
                    continue;
                }
                
                // Check for block data
                MPI_Iprobe(MPI_ANY_SOURCE, TAG_BLOCK_DATA, MPI_COMM_WORLD, &flag, &status);
                if (flag) {
                    int dataSize;
                    MPI_Recv(&dataSize, 1, MPI_INT, status.MPI_SOURCE, TAG_BLOCK_DATA, 
                            MPI_COMM_WORLD, &status);
                    
                    std::vector<char> buffer(dataSize);
                    MPI_Recv(buffer.data(), dataSize, MPI_CHAR, status.MPI_SOURCE, 
                            TAG_BLOCK_DATA, MPI_COMM_WORLD, &status);
                    
                    std::string blockData(buffer.data(), dataSize);
                    std::lock_guard<std::mutex> lock(resultMutex);
                    if (!resultSet) {
                        result = deserializeBlock(blockData);
                        resultSet = true;
                        stopMining.store(true, std::memory_order_release);
                    }
                    continue;
                }
                
                // Small sleep to avoid busy waiting
                std::this_thread::sleep_for(std::chrono::milliseconds(10));
            }
        });
        
        // Worker function for each thread
        auto worker = [&](int threadId) {
            // Thread's starting nonce: processOffset + threadId
            // Thread's stride: totalStride
            int startNonce = processOffset + threadId;
            int nonce = startNonce;
            
            while (!stopMining.load(std::memory_order_relaxed) && 
                   !found.load(std::memory_order_relaxed)) {
                
                Block candidate(index, data, timestamp, prevHash, difficulty, nonce, false);
                std::string hash = candidate.computeHash();
                
                if (hasLeadingZeros(hash, difficulty)) {
                    // Found valid nonce!
                    bool expected = false;
                    if (found.compare_exchange_strong(expected, true, std::memory_order_release, std::memory_order_relaxed)) {
                        // This thread won the race - set the result
                        std::lock_guard<std::mutex> lock(resultMutex);
                        if (!resultSet) {
                            candidate.hash = hash;
                            result = candidate;
                            resultSet = true;
                            stopMining.store(true, std::memory_order_release);
                        }
                        
                        // Broadcast found signal to all other processes
                        int foundSignal = 1;
                        for (int dest = 0; dest < size; ++dest) {
                            if (dest != rank) {
                                MPI_Send(&foundSignal, 1, MPI_INT, dest, TAG_FOUND, MPI_COMM_WORLD);
                            }
                        }
                        
                        // Broadcast block data to all processes
                        std::string blockData = serializeBlock(result);
                        int dataSize = static_cast<int>(blockData.size());
                        for (int dest = 0; dest < size; ++dest) {
                            if (dest != rank) {
                                MPI_Send(&dataSize, 1, MPI_INT, dest, TAG_BLOCK_DATA, MPI_COMM_WORLD);
                                MPI_Send(blockData.c_str(), dataSize, MPI_CHAR, dest, TAG_BLOCK_DATA, MPI_COMM_WORLD);
                            }
                        }
                    }
                    break;
                }
                
                // Increment by total stride
                nonce += totalStride;
                
                // Prevent integer overflow
                if (nonce < startNonce) {
                    break;
                }
            }
        };
        
        // Launch worker threads
        std::vector<std::thread> threads;
        threads.reserve(threadsPerProcess);
        
        for (int i = 0; i < threadsPerProcess; ++i) {
            threads.emplace_back(worker, i);
        }
        
        // Wait for all threads to complete
        for (auto& t : threads) {
            t.join();
        }
        
        // Wait for communication thread
        commThread.join();
        
        // If this process didn't find the solution, wait for it
        if (!resultSet) {
            // Wait for block data from the process that found it
            MPI_Status status;
            int dataSize;
            MPI_Recv(&dataSize, 1, MPI_INT, MPI_ANY_SOURCE, TAG_BLOCK_DATA, 
                    MPI_COMM_WORLD, &status);
            
            std::vector<char> buffer(dataSize);
            MPI_Recv(buffer.data(), dataSize, MPI_CHAR, status.MPI_SOURCE, 
                    TAG_BLOCK_DATA, MPI_COMM_WORLD, &status);
            
            std::string blockData(buffer.data(), dataSize);
            result = deserializeBlock(blockData);
            resultSet = true;
        }
        
        // Final synchronization: ensure all processes have the same block
        // Rank 0 broadcasts the result to ensure consistency
        if (rank == 0) {
            // Rank 0 broadcasts its result
            std::string blockData = serializeBlock(result);
            int dataSize = static_cast<int>(blockData.size());
            MPI_Bcast(&dataSize, 1, MPI_INT, 0, MPI_COMM_WORLD);
            MPI_Bcast(const_cast<char*>(blockData.data()), dataSize, MPI_CHAR, 0, MPI_COMM_WORLD);
        } else {
            // Other ranks receive from rank 0
            int dataSize;
            MPI_Bcast(&dataSize, 1, MPI_INT, 0, MPI_COMM_WORLD);
            std::vector<char> buffer(dataSize);
            MPI_Bcast(buffer.data(), dataSize, MPI_CHAR, 0, MPI_COMM_WORLD);
            std::string blockData(buffer.data(), dataSize);
            result = deserializeBlock(blockData);
        }
        
        return result;
    }
#else
    // Fallback when MPI is not available
    Block mineBlockMPI(
        int index,
        const std::string& data,
        const std::string& prevHash,
        int difficulty,
        int threadsPerProcess) {
        
        // Fall back to sequential mining if MPI is not available
        auto timestamp = std::chrono::system_clock::now();
        int nonce = 0;
        std::string hash;
        
        while (true) {
            Block candidate(index, data, timestamp, prevHash, difficulty, nonce, false);
            hash = candidate.computeHash();
            
            if (hasLeadingZeros(hash, difficulty)) {
                candidate.hash = hash;
                return candidate;
            }
            
            ++nonce;
        }
    }
#endif
}

