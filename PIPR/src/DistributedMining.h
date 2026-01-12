#pragma once

#include <string>
#include <vector>
#include <chrono>
#include "Block.h"

namespace DistributedMining {
    // Session state for mining coordination
    struct MiningSession {
        uint64_t session_id;
        std::string snapshot_tip_hash;
        int snapshot_height;
        int difficulty;
        std::chrono::steady_clock::time_point start_time;
        bool winner_set;
        int winner_rank;
        Block winner_block;
    };
    
    // MPI-based distributed mining function
    // Rank 0 coordinates, all ranks mine, first valid candidate wins
    // Returns the mined block (same on all processes)
    Block mineBlockMPI(
        int index,
        const std::string& data,
        const std::string& prevHash,
        int difficulty,
        int threadsPerProcess,
        const std::vector<Block>& fullChain,
        bool verbose = false
    );
    
    // Lightweight snapshot agreement before mining
    // Returns true if local chain matches snapshot, false if mismatch (worker should request full chain)
    bool syncSnapshotMPI(
        const std::string& localTipHash,
        int localHeight,
        bool verbose = false
    );
    
    // Distribute winning block to all workers (block-only, not full chain)
    // Called by rank 0 after committing block locally
    void distributeWinnerBlockMPI(
        const Block& winnerBlock,
        const std::vector<Block>& fullChain,
        bool verbose = false
    );
    
    // Worker receives and validates winner block
    // Returns true if block was successfully appended
    bool receiveWinnerBlockMPI(
        const std::string& expectedPrevHash,
        Block& outBlock,
        bool verbose = false
    );
    
    // Request full chain from rank 0 (used when snapshot mismatch detected)
    std::vector<Block> requestFullChainMPI(bool verbose = false);
    
    // Send full chain to requesting worker (called by rank 0)
    void sendFullChainMPI(const std::vector<Block>& chain, int requesterRank, bool verbose = false);
    
    // Broadcast full chain to all MPI ranks (called by rank 0 when chain updated from TCP peer)
    // Returns the chain (same for root, received chain for workers)
    std::vector<Block> broadcastChainToAllRanksMPI(const std::vector<Block>& chain, bool verbose = false);
    
    // Workers call this to check for and receive chain updates from rank 0
    // Returns empty if no update available, or the updated chain
    std::vector<Block> checkForChainUpdateMPI(bool verbose = false);
}
