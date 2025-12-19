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
}

