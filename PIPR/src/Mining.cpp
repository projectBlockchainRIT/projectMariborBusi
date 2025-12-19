#include "Mining.h"

#include <chrono>
#include <string>

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
}

