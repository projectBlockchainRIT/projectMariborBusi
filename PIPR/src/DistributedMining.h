#pragma once

#include <string>
#include "Block.h"

namespace DistributedMining {
    // MPI-based distributed mining function
    // Combines MPI processes (nodes) with multi-threading within each process
    // Returns the mined block, which should be the same on all processes
    Block mineBlockMPI(
        int index,
        const std::string& data,
        const std::string& prevHash,
        int difficulty,
        int threadsPerProcess
    );
}

