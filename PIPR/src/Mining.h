#pragma once

#include <string>
#include "Block.h"

namespace Mining {
    // Sequential mining function (no threads, no MPI)
    // Finds a nonce that produces a hash with the required number of leading zeros
    Block mineBlock(
        int index,
        const std::string& data,
        const std::string& prevHash,
        int difficulty
    );

    // Multi-threaded mining function
    // Finds a nonce that produces a hash with the required number of leading zeros
    // Uses numThreads threads to search the nonce space in parallel
    Block mineBlockThreaded(
        int index,
        const std::string& data,
        const std::string& prevHash,
        int difficulty,
        int numThreads
    );
}

