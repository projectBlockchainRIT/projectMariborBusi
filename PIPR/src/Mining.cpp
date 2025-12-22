#include "Mining.h"

#include <atomic>
#include <chrono>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

#include "Block.h"
#include "Crypto.h"

namespace Mining {
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
    }

    Block mineBlock(
        int index,
        const std::string& data,
        const std::string& prevHash,
        int difficulty) {
        
        auto timestamp = std::chrono::system_clock::now();
        int nonce = 0;
        std::string hash;
        
        // Sequential mining: try nonces until we find one that satisfies difficulty
        while (true) {
            Block candidate(index, data, timestamp, prevHash, difficulty, nonce, false);
            hash = candidate.computeHash();
            
            if (hasLeadingZeros(hash, difficulty)) {
                // Found valid nonce
                candidate.hash = hash;
                return candidate;
            }
            
            ++nonce;
        }
    }

    Block mineBlockThreaded(
        int index,
        const std::string& data,
        const std::string& prevHash,
        int difficulty,
        int numThreads) {
        
        if (numThreads <= 1) {
            // Fallback to sequential mining for single thread
            return mineBlock(index, data, prevHash, difficulty);
        }

        auto timestamp = std::chrono::system_clock::now();
        
        // Shared state for coordination between threads
        std::atomic<bool> found(false);
        std::mutex resultMutex;
        Block result(index, data, timestamp, prevHash, difficulty, 0, false);
        bool resultSet = false;

        // Worker function for each thread
        auto worker = [&](int threadId, int stride) {
            int startNonce = threadId;
            int nonce = startNonce;
            
            while (!found.load(std::memory_order_relaxed)) {
                Block candidate(index, data, timestamp, prevHash, difficulty, nonce, false);
                std::string hash = candidate.computeHash();
                
                if (hasLeadingZeros(hash, difficulty)) {
                    // Found valid nonce - try to claim it
                    bool expected = false;
                    if (found.compare_exchange_strong(expected, true, std::memory_order_release, std::memory_order_relaxed)) {
                        // This thread won the race - set the result
                        std::lock_guard<std::mutex> lock(resultMutex);
                        if (!resultSet) {
                            candidate.hash = hash;
                            result = candidate;
                            resultSet = true;
                        }
                    }
                    // Even if we lost the race, we found a valid nonce, so we can exit
                    break;
                }
                
                // Increment by stride to avoid overlap with other threads
                nonce += stride;
                
                // Prevent integer overflow (safety check)
                if (nonce < startNonce) {
                    break;
                }
            }
        };

        // Launch worker threads
        std::vector<std::thread> threads;
        threads.reserve(numThreads);
        
        for (int i = 0; i < numThreads; ++i) {
            threads.emplace_back(worker, i, numThreads);
        }

        // Wait for all threads to complete
        for (auto& t : threads) {
            t.join();
        }

        // Ensure we have a valid result
        if (!resultSet) {
            // Fallback: if somehow no thread set the result, use sequential mining
            // This should not happen in practice, but provides safety
            return mineBlock(index, data, prevHash, difficulty);
        }

        return result;
    }
}

