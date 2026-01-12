#include "Mining.h"

#include <atomic>
#include <chrono>
#include <limits>
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

    Block mineBlockThreaded(
        int index,
        const std::string& data,
        const std::string& prevHash,
        int difficulty,
        int numThreads) {
        
        if (numThreads <= 1) {
            return mineBlock(index, data, prevHash, difficulty);
        }

        auto timestamp = std::chrono::system_clock::now();
        
        std::atomic<bool> found(false);
        std::mutex resultMutex;
        Block result(index, data, timestamp, prevHash, difficulty, 0, false);
        bool resultSet = false;

        auto worker = [&](int threadId, int stride) {
            // unsigned arithmetic to avoid undefined behavior on overflow
            // Convert to int only when creating Block (nonce values are typically small)
            unsigned int startNonce = static_cast<unsigned int>(threadId);
            unsigned int uNonce = startNonce;
            const unsigned int maxNonce = static_cast<unsigned int>(std::numeric_limits<int>::max());
            
            while (!found.load(std::memory_order_relaxed)) {
                // Check for overflow before incrementing
                if (uNonce > maxNonce - static_cast<unsigned int>(stride)) {
                    // Wrapped around - restart from beginning of this thread's range
                    uNonce = startNonce;
                }
                
                int nonce = static_cast<int>(uNonce);
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
                
                uNonce += static_cast<unsigned int>(stride);
            }
        };

        std::vector<std::thread> threads;
        threads.reserve(numThreads);
        
        for (int i = 0; i < numThreads; ++i) {
            threads.emplace_back(worker, i, numThreads);
        }

        for (auto& t : threads) {
            t.join();
        }

        if (!resultSet) {
            return mineBlock(index, data, prevHash, difficulty);
        }

        return result;
    }
}

