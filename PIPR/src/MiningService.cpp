#include "MiningService.h"

#include <chrono>
#include <iomanip>
#include <iostream>
#include <optional>
#include <string>

#include "Mining.h"
#include "DistributedMining.h"

#ifdef USE_MPI
#include <mpi.h>
#endif

MiningService::MiningService(Blockchain &blockchain, int numThreads, bool useMPI)
    : blockchain_(blockchain), numThreads_(numThreads), useMPI_(useMPI) {}

std::optional<Block> MiningService::MineBlock(const std::string &data)
{
    std::scoped_lock lock(mineMutex_);

    auto latest = blockchain_.GetLatestBlock();
    auto difficulty = std::max(1, blockchain_.GetAdjustedDifficulty());

    // Start timing for benchmark
    auto startTime = std::chrono::high_resolution_clock::now();

    Block newBlock = [&]() -> Block {
#ifdef USE_MPI
        if (useMPI_) {
            // Use MPI-based distributed mining
            return DistributedMining::mineBlockMPI(
                latest.index + 1,
                data,
                latest.hash,
                difficulty,
                numThreads_
            );
        } else {
#endif
            // Use threaded mining if numThreads > 1, otherwise use sequential
            return (numThreads_ > 1) 
                ? Mining::mineBlockThreaded(
                    latest.index + 1,
                    data,
                    latest.hash,
                    difficulty,
                    numThreads_
                )
                : Mining::mineBlock(
                    latest.index + 1,
                    data,
                    latest.hash,
                    difficulty
                );
#ifdef USE_MPI
        }
#endif
    }();

    // End timing and calculate duration
    auto endTime = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(endTime - startTime);
    
    // Print benchmark information
    std::cout << "Mining time: " << duration.count() << " ms (threads: " << numThreads_ 
              << ", difficulty: " << difficulty << ", nonce: " << newBlock.nonce << ")\n";

    if (blockchain_.AddBlock(newBlock))
    {
        if (blockMinedHandler_)
        {
            blockMinedHandler_(newBlock);
        }
        return newBlock;
    }

    return std::nullopt;
}

void MiningService::SetBlockMinedHandler(BlockMinedHandler handler)
{
    blockMinedHandler_ = std::move(handler);
}
