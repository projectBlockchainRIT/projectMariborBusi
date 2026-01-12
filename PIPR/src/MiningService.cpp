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

std::optional<Block> MiningService::MineBlock(const std::string &data, bool verbose)
{
    std::scoped_lock lock(mineMutex_);

#ifdef USE_MPI
    // Check for chain updates from rank 0 (TCP peer updates) before mining
    // This ensures rank 1 always has the latest chain before starting to mine
    if (useMPI_) {
        int rank, size;
        MPI_Comm_rank(MPI_COMM_WORLD, &rank);
        MPI_Comm_size(MPI_COMM_WORLD, &size);
        
        if (rank != 0 && size > 1) {
            auto chainUpdate = DistributedMining::checkForChainUpdateMPI(verbose);
            if (!chainUpdate.empty()) {
                blockchain_.ReplaceChain(chainUpdate);
                if (verbose) {
                    std::cout << "[rank " << rank << "/" << size << "] Updated chain before mining (size=" 
                              << chainUpdate.size() << ")\n" << std::flush;
                }
            }
        }
    }
#endif

    auto latest = blockchain_.GetLatestBlock();
    auto difficulty = std::max(1, blockchain_.GetAdjustedDifficulty());

    auto startTime = std::chrono::high_resolution_clock::now();

    Block newBlock = [&]() -> Block {
#ifdef USE_MPI
        if (useMPI_) {
            auto chain = blockchain_.GetChain();
            return DistributedMining::mineBlockMPI(
                latest.index + 1,
                data,
                latest.hash,
                difficulty,
                numThreads_,
                chain,
                verbose
            );
        } else {
#endif
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

    auto endTime = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(endTime - startTime);
    
#ifdef USE_MPI
    int rank = 0;
    if (useMPI_) {
        MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    }
    if (rank == 0 || verbose) {
        std::cout << "[DEBUG Rank " << rank << "] Mining time: " << duration.count() << " ms (threads: " << numThreads_ 
                  << ", difficulty: " << difficulty << ", nonce: " << newBlock.nonce << ")\n";
    }
#else
    std::cout << "Mining time: " << duration.count() << " ms (threads: " << numThreads_ 
              << ", difficulty: " << difficulty << ", nonce: " << newBlock.nonce << ")\n";
#endif

    if (blockchain_.AddBlock(newBlock))
    {
        if (blockMinedHandler_)
        {
            blockMinedHandler_(newBlock);
        }
        return newBlock;
    }

#ifdef USE_MPI
    if (useMPI_) {
        if (verbose) {
            std::cout << "[DEBUG Rank " << rank << "] AddBlock failed for block " << newBlock.index 
                      << " (likely mined by another rank) - will be synchronized via chain sync\n";
        }
        return newBlock;
    }
#endif

    return std::nullopt;
}

void MiningService::SetBlockMinedHandler(BlockMinedHandler handler)
{
    blockMinedHandler_ = std::move(handler);
}