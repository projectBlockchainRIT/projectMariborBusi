#include "DistributedMining.h"

#include <atomic>
#include <chrono>
#include <limits>
#include <mutex>
#include <sstream>
#include <string>
#include <thread>
#include <vector>
#include <iostream>
#include <iomanip>
#include <random>

#ifdef USE_MPI
#include <mpi.h>
#endif

#include "Block.h"
#include "Crypto.h"
#include "JsonUtils.h"

namespace DistributedMining
{
    namespace
    {
        const int TAG_CMD_MINE = 1;
        const int TAG_STOP_MINING = 2;
        const int TAG_BLOCK_FOUND = 3;        // Control: foundRank (MPI_INT)
        const int TAG_BLOCK_LEN = 10;         // Control: block length (uint32_t)
        const int TAG_BLOCK_BYTES = 11;       // Data: block bytes (MPI_CHAR)
        const int TAG_BLOCK_WINNER = 4;       // ACK from workers (MPI_INT)
        const int TAG_BLOCK_WINNER_LEN = 12;  // Winner block length (MPI_INT)
        const int TAG_BLOCK_WINNER_DATA = 13; // Winner block data (MPI_CHAR)
        const int TAG_CHAIN_REQUEST = 5;
        const int TAG_CHAIN_RESPONSE = 6;
        const int TAG_MINING_SNAPSHOT = 7;
        const int TAG_SNAPSHOT_MISMATCH = 8;
        const int TAG_CHAIN_BROADCAST = 9;

        std::chrono::steady_clock::time_point lastLogTime[64] = {};
        const std::chrono::milliseconds LOG_RATE_LIMIT(1000);

        bool shouldLog(int rank, bool verbose)
        {
            if (!verbose)
                return false;
            auto now = std::chrono::steady_clock::now();
            if (rank < 64)
            {
                if (now - lastLogTime[rank] < LOG_RATE_LIMIT)
                {
                    return false;
                }
                lastLogTime[rank] = now;
            }
            return true;
        }

        void logMessage(int rank, int size, const std::string &msg, bool verbose = true)
        {
            if (!verbose && rank != 0)
                return; // Only rank 0 logs in non-verbose mode
            auto now = std::chrono::steady_clock::now();
            auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(
                          now.time_since_epoch())
                          .count();
            std::cout << "[rank " << rank << "/" << size << "][t=" << ms << "] " << msg << "\n"
                      << std::flush;
        }

        bool hasLeadingZeros(const std::string &hash, int difficulty)
        {
            if (static_cast<int>(hash.size()) < difficulty)
            {
                return false;
            }
            for (int i = 0; i < difficulty; ++i)
            {
                if (hash[i] != '0')
                {
                    return false;
                }
            }
            return true;
        }

        std::string serializeBlock(const Block &block)
        {
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

        Block deserializeBlock(const std::string &serialized)
        {
            std::istringstream iss(serialized);
            std::string token;
            std::vector<std::string> tokens;

            while (std::getline(iss, token, '|'))
            {
                tokens.push_back(token);
            }

            if (tokens.size() != 7)
            {
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

        uint64_t generateSessionId()
        {
            static std::random_device rd;
            static std::mt19937_64 gen(rd());
            return gen();
        }
    }

#ifdef USE_MPI
    bool syncSnapshotMPI(const std::string &localTipHash, int localHeight, bool verbose)
    {
        int rank, size;
        MPI_Comm_rank(MPI_COMM_WORLD, &rank);
        MPI_Comm_size(MPI_COMM_WORLD, &size);

        if (size == 1)
            return true;

        if (rank == 0)
        {
            int snapshotHeight = localHeight;
            int hashLen = static_cast<int>(localTipHash.size());

            MPI_Bcast(&snapshotHeight, 1, MPI_INT, 0, MPI_COMM_WORLD);
            MPI_Bcast(&hashLen, 1, MPI_INT, 0, MPI_COMM_WORLD);
            MPI_Bcast(const_cast<char *>(localTipHash.c_str()), hashLen, MPI_CHAR, 0, MPI_COMM_WORLD);

            if (verbose)
            {
                logMessage(rank, size, "[SYNC] Sent snapshot (height=" + std::to_string(snapshotHeight) + ", hash=" + localTipHash.substr(0, 16) + "...)", verbose);
            }
            return true;
        }
        else
        {
            int snapshotHeight, hashLen;
            MPI_Bcast(&snapshotHeight, 1, MPI_INT, 0, MPI_COMM_WORLD);
            MPI_Bcast(&hashLen, 1, MPI_INT, 0, MPI_COMM_WORLD);

            std::vector<char> hashBuf(hashLen);
            MPI_Bcast(hashBuf.data(), hashLen, MPI_CHAR, 0, MPI_COMM_WORLD);
            std::string snapshotHash(hashBuf.data(), hashLen);

            bool matches = (snapshotHeight == localHeight && snapshotHash == localTipHash);

            if (!matches)
            {
                int mismatch = 1;
                MPI_Send(&mismatch, 1, MPI_INT, 0, TAG_SNAPSHOT_MISMATCH, MPI_COMM_WORLD);
                if (verbose)
                {
                    logMessage(rank, size, "[SYNC] Snapshot mismatch - requesting full chain", verbose);
                }
                return false;
            }

            if (verbose)
            {
                logMessage(rank, size, "[SYNC] Snapshot matches local chain", verbose);
            }
            return true;
        }
    }

    std::vector<Block> requestFullChainMPI(bool verbose)
    {
        int rank, size;
        MPI_Comm_rank(MPI_COMM_WORLD, &rank);
        MPI_Comm_size(MPI_COMM_WORLD, &size);

        if (rank == 0)
            return {};

        int request = 1;
        MPI_Send(&request, 1, MPI_INT, 0, TAG_CHAIN_REQUEST, MPI_COMM_WORLD);

        int dataSize = 0;
        MPI_Status status;
        MPI_Recv(&dataSize, 1, MPI_INT, 0, TAG_CHAIN_RESPONSE, MPI_COMM_WORLD, &status);

        if (dataSize <= 0 || dataSize >= 10000000) // Sanity check
            return {};

        std::vector<char> buffer(dataSize);
        MPI_Recv(buffer.data(), dataSize, MPI_CHAR, 0, TAG_CHAIN_RESPONSE, MPI_COMM_WORLD, &status);

        std::string chainData(buffer.data(), dataSize);
        auto chainOpt = DeserializeChain(chainData);

        if (chainOpt && verbose)
        {
            logMessage(rank, size, "[SYNC] Received full chain (size=" + std::to_string(chainOpt->size()) + ")", verbose);
        }

        return chainOpt ? *chainOpt : std::vector<Block>();
    }

    void sendFullChainMPI(const std::vector<Block> &chain, int requesterRank, bool verbose)
    {
        int rank, size;
        MPI_Comm_rank(MPI_COMM_WORLD, &rank);
        MPI_Comm_size(MPI_COMM_WORLD, &size);

        if (rank != 0)
            return;

        std::string chainData = SerializeChain(chain);
        int dataSize = static_cast<int>(chainData.size());

        MPI_Send(&dataSize, 1, MPI_INT, requesterRank, TAG_CHAIN_RESPONSE, MPI_COMM_WORLD);
        MPI_Send(const_cast<char *>(chainData.c_str()), dataSize, MPI_CHAR, requesterRank, TAG_CHAIN_RESPONSE, MPI_COMM_WORLD);

        if (verbose)
        {
            logMessage(rank, size, "[SYNC] Sent full chain to rank " + std::to_string(requesterRank), verbose);
        }
    }

    std::vector<Block> broadcastChainToAllRanksMPI(const std::vector<Block> &chain, bool verbose)
    {
        int rank, size;
        MPI_Comm_rank(MPI_COMM_WORLD, &rank);
        MPI_Comm_size(MPI_COMM_WORLD, &size);

        if (size == 1)
            return chain;

        if (rank == 0)
        {
            // Rank 0 sends chain to all worker ranks using MPI_Send (non-blocking for receivers)
            logMessage(rank, size, "[SYNC] Broadcasting chain to all workers (size=" + std::to_string(chain.size()) + ")", false);

            std::string chainData = SerializeChain(chain);
            int dataSize = static_cast<int>(chainData.size());

            // Send chain to all worker ranks (1 to size-1)
            for (int dest = 1; dest < size; ++dest)
            {
                MPI_Send(&dataSize, 1, MPI_INT, dest, TAG_CHAIN_BROADCAST, MPI_COMM_WORLD);
                MPI_Send(const_cast<char *>(chainData.c_str()), dataSize, MPI_CHAR, dest, TAG_CHAIN_BROADCAST, MPI_COMM_WORLD);
            }

            logMessage(rank, size, "[SYNC] Chain broadcast complete", false);
            return chain;
        }
        else
        {
            // Worker ranks receive chain via checkForChainUpdateMPI (non-blocking probe)
            // This function just returns the chain for rank 0, workers use checkForChainUpdateMPI
            return chain;
        }
    }

    std::vector<Block> checkForChainUpdateMPI(bool verbose)
    {
        int rank, size;
        MPI_Comm_rank(MPI_COMM_WORLD, &rank);
        MPI_Comm_size(MPI_COMM_WORLD, &size);

        if (rank == 0 || size == 1)
            return {};

        // Drain ALL chain update messages from queue and use only the latest (longest) one
        // This prevents receiving stale chain updates when multiple updates arrive in sequence
        std::vector<Block> bestChain;
        int flag = 1;
        MPI_Status status;

        while (flag)
        {
            MPI_Iprobe(0, TAG_CHAIN_BROADCAST, MPI_COMM_WORLD, &flag, &status);
            if (flag)
            {
                int dataSize = 0;
                MPI_Recv(&dataSize, 1, MPI_INT, 0, TAG_CHAIN_BROADCAST, MPI_COMM_WORLD, &status);

                if (dataSize > 0 && dataSize < 10000000) // Sanity check
                {
                    std::vector<char> buffer(dataSize);
                    MPI_Recv(buffer.data(), dataSize, MPI_CHAR, 0, TAG_CHAIN_BROADCAST, MPI_COMM_WORLD, &status);

                    std::string chainData(buffer.data(), dataSize);
                    auto chainOpt = DeserializeChain(chainData);

                    if (chainOpt && chainOpt->size() > bestChain.size())
                    {
                        // Keep only the longest chain (most recent)
                        bestChain = *chainOpt;
                        if (verbose)
                        {
                            logMessage(rank, size, "[SYNC] Received chain update from root (size=" + std::to_string(chainOpt->size()) + ")", verbose);
                        }
                    }
                    else if (chainOpt && verbose)
                    {
                        logMessage(rank, size, "[SYNC] Drained stale chain update (size=" + std::to_string(chainOpt->size()) + ", keeping size=" + std::to_string(bestChain.size()) + ")", verbose);
                    }
                }
            }
        }

        return bestChain;
    }

    void distributeWinnerBlockMPI(const Block &winnerBlock, const std::vector<Block> &fullChain, bool verbose)
    {
        int rank, size;
        MPI_Comm_rank(MPI_COMM_WORLD, &rank);
        MPI_Comm_size(MPI_COMM_WORLD, &size);

        if (rank != 0)
            return;

        std::string blockData = serializeBlock(winnerBlock);
        int dataSize = static_cast<int>(blockData.size());

        logMessage(rank, size, "[SYNC] Broadcasting winner block (size=" + std::to_string(dataSize) + ")", false);

        // Send winner block to each worker rank using MPI_Send (non-blocking for receivers)
        for (int dest = 1; dest < size; ++dest)
        {
            MPI_Send(&dataSize, 1, MPI_INT, dest, TAG_BLOCK_WINNER_LEN, MPI_COMM_WORLD);
            MPI_Send(const_cast<char *>(blockData.c_str()), dataSize, MPI_CHAR, dest, TAG_BLOCK_WINNER_DATA, MPI_COMM_WORLD);
        }

        logMessage(rank, size, "[SYNC] Waiting for ACKs from " + std::to_string(size - 1) + " worker(s)...", false);
        for (int dest = 1; dest < size; ++dest)
        {
            int ack;
            MPI_Status status;
            logMessage(rank, size, "[SYNC] Waiting for ACK from rank " + std::to_string(dest) + "...", false);
            MPI_Recv(&ack, 1, MPI_INT, dest, TAG_BLOCK_WINNER, MPI_COMM_WORLD, &status);
            logMessage(rank, size, "[SYNC] Received ACK from rank " + std::to_string(dest) + " (ack=" + std::to_string(ack) + ")", false);

            if (ack == 0)
            {
                logMessage(rank, size, "[SYNC] Rank " + std::to_string(dest) + " requested full chain, sending...", false);
                sendFullChainMPI(fullChain, dest, verbose);
            }
        }
    }

    bool receiveWinnerBlockMPI(const std::string &expectedPrevHash, Block &outBlock, bool verbose)
    {
        int rank, size;
        MPI_Comm_rank(MPI_COMM_WORLD, &rank);
        MPI_Comm_size(MPI_COMM_WORLD, &size);

        if (rank == 0)
            return false;

        int dataSize = 0;
        MPI_Bcast(&dataSize, 1, MPI_INT, 0, MPI_COMM_WORLD);

        if (dataSize <= 0 || dataSize >= 1000000)
        {
            if (verbose)
            {
                logMessage(rank, size, "[SYNC] Invalid winner block size: " + std::to_string(dataSize), verbose);
            }
            return false;
        }

        std::vector<char> buffer(dataSize);
        MPI_Bcast(buffer.data(), dataSize, MPI_CHAR, 0, MPI_COMM_WORLD);

        std::string blockData(buffer.data(), dataSize);
        Block receivedBlock = deserializeBlock(blockData);

        if (receivedBlock.previousHash == expectedPrevHash)
        {
            outBlock = receivedBlock;
            int ack = 1;
            MPI_Send(&ack, 1, MPI_INT, 0, TAG_BLOCK_WINNER, MPI_COMM_WORLD);
            if (verbose)
            {
                logMessage(rank, size, "[SYNC] Winner block validated and appended", verbose);
            }
            return true;
        }
        else
        {
            int ack = 0;
            MPI_Send(&ack, 1, MPI_INT, 0, TAG_BLOCK_WINNER, MPI_COMM_WORLD);
            if (verbose)
            {
                logMessage(rank, size, "[SYNC] Winner block prevHash mismatch - requesting full chain", verbose);
            }
            return false;
        }
    }

    Block mineBlockMPI(
        int index,
        const std::string &data,
        const std::string &prevHash,
        int difficulty,
        int threadsPerProcess,
        const std::vector<Block> &fullChain,
        bool verbose)
    {

        int rank, size;
        MPI_Comm_rank(MPI_COMM_WORLD, &rank);
        MPI_Comm_size(MPI_COMM_WORLD, &size);

        if (size == 1)
        {
            auto timestamp = std::chrono::system_clock::now();
            std::atomic<bool> found(false);
            std::mutex resultMutex;
            Block result(index, data, timestamp, prevHash, difficulty, 0, false);
            bool resultSet = false;

            auto worker = [&](int threadId, int stride)
            {
                // unsigned arithmetic to avoid undefined behavior on overflow
                unsigned int startNonce = static_cast<unsigned int>(threadId);
                unsigned int uNonce = startNonce;
                const unsigned int maxNonce = static_cast<unsigned int>(std::numeric_limits<int>::max());

                while (!found.load(std::memory_order_relaxed))
                {
                    // Check for overflow before incrementing
                    if (uNonce > maxNonce - static_cast<unsigned int>(stride)) {
                        // Wrapped around - restart from beginning of this thread's range
                        uNonce = startNonce;
                    }

                    int nonce = static_cast<int>(uNonce);
                    Block candidate(index, data, timestamp, prevHash, difficulty, nonce, false);
                    std::string hash = candidate.computeHash();

                    if (hasLeadingZeros(hash, difficulty))
                    {
                        bool expected = false;
                        if (found.compare_exchange_strong(expected, true, std::memory_order_release, std::memory_order_relaxed))
                        {
                            std::lock_guard<std::mutex> lock(resultMutex);
                            if (!resultSet)
                            {
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
            threads.reserve(threadsPerProcess);

            for (int i = 0; i < threadsPerProcess; ++i)
            {
                threads.emplace_back(worker, i, threadsPerProcess);
            }

            for (auto &t : threads)
            {
                t.join();
            }

            return result;
        }

        uint64_t sessionId = generateSessionId();
        auto sessionStart = std::chrono::steady_clock::now();

        if (rank == 0)
        {
            logMessage(rank, size, "[SYNC] Synchronizing chain BEFORE mining...", false);

            int snapshotHeight = index;
            int hashLen = static_cast<int>(prevHash.size());
            MPI_Bcast(&snapshotHeight, 1, MPI_INT, 0, MPI_COMM_WORLD);
            MPI_Bcast(&hashLen, 1, MPI_INT, 0, MPI_COMM_WORLD);
            MPI_Bcast(const_cast<char *>(prevHash.c_str()), hashLen, MPI_CHAR, 0, MPI_COMM_WORLD);

            int flag = 0;
            MPI_Status status;
            for (int i = 0; i < size - 1; ++i)
            {
                MPI_Iprobe(MPI_ANY_SOURCE, TAG_SNAPSHOT_MISMATCH, MPI_COMM_WORLD, &flag, &status);
                if (flag)
                {
                    int mismatch;
                    MPI_Recv(&mismatch, 1, MPI_INT, status.MPI_SOURCE, TAG_SNAPSHOT_MISMATCH, MPI_COMM_WORLD, &status);
                }
            }
        }
        else
        {
            int snapshotHeight, hashLen;
            MPI_Bcast(&snapshotHeight, 1, MPI_INT, 0, MPI_COMM_WORLD);
            MPI_Bcast(&hashLen, 1, MPI_INT, 0, MPI_COMM_WORLD);

            std::vector<char> hashBuf(hashLen);
            MPI_Bcast(hashBuf.data(), hashLen, MPI_CHAR, 0, MPI_COMM_WORLD);
            std::string snapshotHash(hashBuf.data(), hashLen);
        }

        auto timestamp = std::chrono::system_clock::now();

        int mineParams[3] = {index, difficulty, threadsPerProcess};
        MPI_Bcast(mineParams, 3, MPI_INT, 0, MPI_COMM_WORLD);

        if (rank == 0)
        {
            logMessage(rank, size, "Mining started", false);
        }

        int nonceStride = size;
        int nonceStart = rank;

        if (verbose)
        {
            logMessage(rank, size, "Mining partition: start=" + std::to_string(nonceStart) + ", stride=" + std::to_string(nonceStride) + ", threads=" + std::to_string(threadsPerProcess), verbose);
        }

        std::atomic<bool> found(false);
        std::atomic<bool> stopMining(false);
        std::atomic<int> winnerRank(-1);
        std::mutex resultMutex;
        Block result(index, data, timestamp, prevHash, difficulty, 0, false);
        bool resultSet = false;
        std::chrono::steady_clock::time_point winnerTime;
        std::string winnerHash;

        // Store expected prevHash for chain update detection (rank != 0)
        std::string expectedPrevHash = prevHash;

        // Shared queue for rank 0's own FOUND_BLOCK messages (to avoid MPI_Send to self deadlock)
        std::mutex localFoundMutex;
        std::vector<Block> localFoundBlocks;

        // Drain any leftover STOP signals from previous mining sessions
        // This prevents worker threads from immediately stopping due to stale messages
        if (rank != 0)
        {
            int flag = 1;
            MPI_Status status;
            while (flag)
            {
                MPI_Iprobe(0, TAG_STOP_MINING, MPI_COMM_WORLD, &flag, &status);
                if (flag)
                {
                    int stopSignal;
                    MPI_Recv(&stopSignal, 1, MPI_INT, 0, TAG_STOP_MINING, MPI_COMM_WORLD, &status);
                    if (verbose)
                    {
                        logMessage(rank, size, "[CLEANUP] Drained leftover STOP signal from previous session", verbose);
                    }
                }
            }
        }

        // Drain any leftover FOUND_BLOCK messages from previous sessions (rank 0 only)
        if (rank == 0)
        {
            int flag = 1;
            MPI_Status status;
            while (flag)
            {
                MPI_Iprobe(MPI_ANY_SOURCE, TAG_BLOCK_FOUND, MPI_COMM_WORLD, &flag, &status);
                if (flag)
                {
                    int foundRank = 0;
                    MPI_Recv(&foundRank, 1, MPI_INT, status.MPI_SOURCE, TAG_BLOCK_FOUND, MPI_COMM_WORLD, &status);
                    int dataSize = 0;
                    MPI_Recv(&dataSize, 1, MPI_INT, status.MPI_SOURCE, TAG_BLOCK_LEN, MPI_COMM_WORLD, &status);
                    if (dataSize > 0 && dataSize < 1000000) // Sanity check
                    {
                        std::vector<uint8_t> buffer(dataSize);
                        MPI_Recv(buffer.data(), dataSize, MPI_CHAR, status.MPI_SOURCE, TAG_BLOCK_BYTES, MPI_COMM_WORLD, &status);
                    }
                    if (verbose)
                    {
                        logMessage(rank, size, "[CLEANUP] Drained leftover FOUND_BLOCK message from rank " + std::to_string(foundRank), verbose);
                    }
                }
            }
        }

        std::thread commThread;
        if (rank == 0)
        {
            commThread = std::thread([&]()
                                     {
                while (!stopMining.load(std::memory_order_relaxed) && !resultSet) {
                    // First check local queue for rank 0's own finds (avoids MPI_Send to self)
                    {
                        std::lock_guard<std::mutex> lock(localFoundMutex);
                        if (!localFoundBlocks.empty()) {
                            Block candidate = localFoundBlocks.front();
                            localFoundBlocks.erase(localFoundBlocks.begin());
                            
                            // Validate the block before accepting it
                            if (candidate.index == index && candidate.previousHash == prevHash &&
                                hasLeadingZeros(candidate.hash, difficulty)) {
                                
                                std::lock_guard<std::mutex> resultLock(resultMutex);
                                if (!resultSet) {
                                    auto now = std::chrono::steady_clock::now();
                                    result = candidate;
                                    resultSet = true;
                                    winnerRank.store(0, std::memory_order_release);
                                    winnerTime = now;
                                    winnerHash = candidate.hash;
                                    stopMining.store(true, std::memory_order_release);
                                    
                                    logMessage(rank, size, "Winner: rank 0 (nonce=" + std::to_string(candidate.nonce) + ")", false);
                                    
                                    // Send STOP signal to all worker ranks (1 to size-1)
                                    // Rank 0's worker threads will stop via stopMining flag
                                    int stopSignal = 1;
                                    for (int dest = 1; dest < size; ++dest) {
                                        MPI_Send(&stopSignal, 1, MPI_INT, dest, TAG_STOP_MINING, MPI_COMM_WORLD);
                                    }
                                }
                            }
                            continue;
                        }
                    }
                    
                    // Check for FOUND_BLOCK messages from other ranks
                    int flag = 0;
                    MPI_Status status;
                    MPI_Iprobe(MPI_ANY_SOURCE, TAG_BLOCK_FOUND, MPI_COMM_WORLD, &flag, &status);
                    if (flag) {
                        int foundRank = 0;
                        MPI_Recv(&foundRank, 1, MPI_INT, status.MPI_SOURCE, TAG_BLOCK_FOUND, 
                                MPI_COMM_WORLD, &status);
                        
                        int dataSize = 0;
                        MPI_Recv(&dataSize, 1, MPI_INT, status.MPI_SOURCE, TAG_BLOCK_LEN, 
                                MPI_COMM_WORLD, &status);
                        if (dataSize <= 0 || dataSize >= 1000000) {
                            if (verbose) {
                                logMessage(rank, size, "Invalid block size from rank " + std::to_string(foundRank) + ": " + std::to_string(dataSize), verbose);
                            }
                            continue;
                        }
                        std::vector<uint8_t> buffer(dataSize);
                        MPI_Recv(buffer.data(), dataSize, MPI_CHAR, status.MPI_SOURCE, 
                                TAG_BLOCK_BYTES, MPI_COMM_WORLD, &status);
                        std::string blockData(reinterpret_cast<const char*>(buffer.data()), dataSize);
                        Block candidate = deserializeBlock(blockData);
                        
                        // Validate the block before accepting it
                        if (candidate.index == index && candidate.previousHash == prevHash &&
                            hasLeadingZeros(candidate.hash, difficulty)) {
                            
                            std::lock_guard<std::mutex> lock(resultMutex);
                            if (!resultSet) {
                                auto now = std::chrono::steady_clock::now();
                                result = candidate;
                                resultSet = true;
                                winnerRank.store(foundRank, std::memory_order_release);
                                winnerTime = now;
                                winnerHash = candidate.hash;
                                stopMining.store(true, std::memory_order_release);
                                
                                logMessage(rank, size, "Winner: rank " + std::to_string(foundRank) + 
                                          " (nonce=" + std::to_string(candidate.nonce) + ")", false);
                                
                                // Send STOP signal to all worker ranks (1 to size-1)
                                // Rank 0's worker threads will stop via stopMining flag
                                int stopSignal = 1;
                                for (int dest = 1; dest < size; ++dest) {
                                    MPI_Send(&stopSignal, 1, MPI_INT, dest, TAG_STOP_MINING, MPI_COMM_WORLD);
                                }
                            } else if (verbose) {
                                logMessage(rank, size, "Late/stale BLOCK_FOUND from rank " + std::to_string(foundRank), verbose);
                            }
                        } else if (verbose) {
                            logMessage(rank, size, "Invalid BLOCK_FOUND from rank " + std::to_string(foundRank) + " (validation failed)", verbose);
                        }
                        continue;
                    }
                    
                    MPI_Iprobe(MPI_ANY_SOURCE, TAG_CHAIN_REQUEST, MPI_COMM_WORLD, &flag, &status);
                    if (flag) {
                        int request;
                        MPI_Recv(&request, 1, MPI_INT, status.MPI_SOURCE, TAG_CHAIN_REQUEST, 
                                MPI_COMM_WORLD, &status);
                    }
                    
                    // Yield to other threads instead of sleeping - reduces latency
                    std::this_thread::yield();
                } });
        }

        auto worker = [&](int threadId)
        {
            // Use unsigned arithmetic to avoid undefined behavior on overflow
            int threadStride = nonceStride * threadsPerProcess;
            unsigned int startNonce = static_cast<unsigned int>(nonceStart + threadId * nonceStride);
            unsigned int uNonce = startNonce;
            const unsigned int maxNonce = static_cast<unsigned int>(std::numeric_limits<int>::max());

            while (!stopMining.load(std::memory_order_relaxed) &&
                   !found.load(std::memory_order_relaxed))
            {
                // Check for overflow before incrementing
                if (uNonce > maxNonce - static_cast<unsigned int>(threadStride)) {
                    // Wrapped around - restart from beginning of this thread's range
                    uNonce = startNonce;
                }
                // Check for STOP signal from rank 0 (for non-zero ranks)
                // Rank 0's worker threads stop via stopMining flag set by commThread
                if (rank != 0)
                {
                    int flag = 0;
                    MPI_Status status;
                    MPI_Iprobe(0, TAG_STOP_MINING, MPI_COMM_WORLD, &flag, &status);
                    if (flag)
                    {
                        int stopSignal;
                        MPI_Recv(&stopSignal, 1, MPI_INT, 0, TAG_STOP_MINING, MPI_COMM_WORLD, &status);
                        stopMining.store(true, std::memory_order_release);
                        if (verbose)
                        {
                            logMessage(rank, size, "[WORKER] Received STOP signal, exiting mining loop", verbose);
                        }
                        break;
                    }

                    // Check for chain updates from rank 0 (TCP peer updates)
                    // If chain updated, prevHash is stale - stop mining
                    MPI_Iprobe(0, TAG_CHAIN_BROADCAST, MPI_COMM_WORLD, &flag, &status);
                    if (flag)
                    {
                        stopMining.store(true, std::memory_order_release);
                        if (verbose)
                        {
                            logMessage(rank, size, "[WORKER] Chain update detected during mining - stopping (prevHash stale)", verbose);
                        }
                        break;
                    }
                }

                int nonce = static_cast<int>(uNonce);
                Block candidate(index, data, timestamp, prevHash, difficulty, nonce, false);
                std::string hash = candidate.computeHash();

                if (hasLeadingZeros(hash, difficulty))
                {
                    bool expected = false;
                    if (found.compare_exchange_strong(expected, true, std::memory_order_release, std::memory_order_relaxed))
                    {
                        candidate.hash = hash;

                        if (rank == 0)
                        {
                            // For rank 0, add to local queue (avoids MPI_Send to self deadlock)
                            std::lock_guard<std::mutex> lock(localFoundMutex);
                            localFoundBlocks.push_back(candidate);
                            if (verbose)
                            {
                                logMessage(rank, size, "Found block! Added to local queue (nonce=" + std::to_string(nonce) + ")", verbose);
                            }
                        }
                        else
                        {
                            // For other ranks, send FOUND_BLOCK message to rank 0
                            if (verbose)
                            {
                                logMessage(rank, size, "Found block! Notifying root (nonce=" + std::to_string(nonce) + ")", verbose);
                            }
                            std::string blockData = serializeBlock(candidate);
                            int dataSize = static_cast<int>(blockData.size());
                            int foundRank = rank;

                            MPI_Send(&foundRank, 1, MPI_INT, 0, TAG_BLOCK_FOUND, MPI_COMM_WORLD);
                            MPI_Send(&dataSize, 1, MPI_INT, 0, TAG_BLOCK_LEN, MPI_COMM_WORLD);
                            if (dataSize > 0)
                            {
                                MPI_Send(blockData.c_str(), dataSize, MPI_CHAR, 0, TAG_BLOCK_BYTES, MPI_COMM_WORLD);
                            }
                        }
                    }
                    // Don't break here - continue mining until STOP signal is received
                    // This ensures we don't stop prematurely before rank 0 validates the block
                }

                uNonce += static_cast<unsigned int>(threadStride);
            }
        };

        std::vector<std::thread> threads;
        threads.reserve(threadsPerProcess);

        for (int i = 0; i < threadsPerProcess; ++i)
        {
            threads.emplace_back(worker, i);
        }

        for (auto &t : threads)
        {
            t.join();
        }

        if (rank != 0)
        {
            logMessage(rank, size, "[SYNC] All worker threads joined, checking for STOP signal...", false);
            int flag = 0;
            MPI_Status status;

            // First check for chain update (has priority - means prevHash is stale)
            MPI_Iprobe(0, TAG_CHAIN_BROADCAST, MPI_COMM_WORLD, &flag, &status);
            if (flag)
            {
                // Chain was updated during mining - prevHash is stale
                // Receive the chain update and return early (mining will restart with new prevHash)
                int dataSize = 0;
                MPI_Recv(&dataSize, 1, MPI_INT, 0, TAG_CHAIN_BROADCAST, MPI_COMM_WORLD, &status);
                if (dataSize > 0 && dataSize < 10000000)
                {
                    std::vector<char> buffer(dataSize);
                    MPI_Recv(buffer.data(), dataSize, MPI_CHAR, 0, TAG_CHAIN_BROADCAST, MPI_COMM_WORLD, &status);
                    std::string chainData(buffer.data(), dataSize);
                    auto chainOpt = DeserializeChain(chainData);
                    if (chainOpt && !chainOpt->empty())
                    {
                        logMessage(rank, size, "[SYNC] Mining stopped due to chain update - will restart with new prevHash", false);
                        // Return the latest block from updated chain
                        result = chainOpt->back();
                        return result;
                    }
                }
            }

            // Then check for STOP signal (normal case - block was found)
            MPI_Iprobe(0, TAG_STOP_MINING, MPI_COMM_WORLD, &flag, &status);
            if (flag)
            {
                int stopSignal;
                MPI_Recv(&stopSignal, 1, MPI_INT, 0, TAG_STOP_MINING, MPI_COMM_WORLD, &status);
                logMessage(rank, size, "[SYNC] Received STOP signal after threads joined", false);
            }
            else if (!stopMining.load(std::memory_order_relaxed))
            {
                logMessage(rank, size, "[SYNC] No STOP signal found, proceeding to wait for broadcast", false);
            }
        }

        if (rank == 0 && commThread.joinable())
        {
            commThread.join();
        }

        if (rank == 0)
        {
            if (!resultSet)
            {
                logMessage(rank, size, "ERROR: No winner block found!", false);
                return result;
            }

            if (verbose)
            {
                logMessage(rank, size, "[SYNC] Distributing winner block to workers...", verbose);
            }
            distributeWinnerBlockMPI(result, fullChain, verbose);
            logMessage(rank, size, "MPI sync complete", false);
        }
        else
        {
            logMessage(rank, size, "[SYNC] Waiting for winner block from rank 0...", false);

            // Before receiving winner block, check if chain update arrived (prevHash might be stale)
            int flag = 0;
            MPI_Status status;
            MPI_Iprobe(0, TAG_CHAIN_BROADCAST, MPI_COMM_WORLD, &flag, &status);
            if (flag)
            {
                // Chain update arrived - prevHash is stale, receive chain and return
                int dataSize = 0;
                MPI_Recv(&dataSize, 1, MPI_INT, 0, TAG_CHAIN_BROADCAST, MPI_COMM_WORLD, &status);
                if (dataSize > 0 && dataSize < 10000000)
                {
                    std::vector<char> buffer(dataSize);
                    MPI_Recv(buffer.data(), dataSize, MPI_CHAR, 0, TAG_CHAIN_BROADCAST, MPI_COMM_WORLD, &status);
                    std::string chainData(buffer.data(), dataSize);
                    auto chainOpt = DeserializeChain(chainData);
                    if (chainOpt && !chainOpt->empty())
                    {
                        logMessage(rank, size, "[SYNC] Chain update received before winner block - prevHash stale, returning updated chain tip", false);
                        result = chainOpt->back();
                        // Send ACK=0 to indicate we need full chain (though we already have it)
                        int ack = 0;
                        MPI_Send(&ack, 1, MPI_INT, 0, TAG_BLOCK_WINNER, MPI_COMM_WORLD);
                        return result;
                    }
                }
            }

            // Receive winner block using MPI_Recv (instead of MPI_Bcast)
            int dataSize = 0;
            MPI_Status recvStatus;
            MPI_Recv(&dataSize, 1, MPI_INT, 0, TAG_BLOCK_WINNER_LEN, MPI_COMM_WORLD, &recvStatus);
            logMessage(rank, size, "[SYNC] Received winner block size: " + std::to_string(dataSize), false);

            if (dataSize <= 0 || dataSize >= 1000000)
            {
                logMessage(rank, size, "[SYNC] ERROR: Invalid winner block size: " + std::to_string(dataSize), false);
                int ack = 0;
                MPI_Send(&ack, 1, MPI_INT, 0, TAG_BLOCK_WINNER, MPI_COMM_WORLD);
                auto fullChain = requestFullChainMPI(verbose);
                if (!fullChain.empty())
                {
                    result = fullChain.back();
                }
                return result;
            }

            try
            {
                // Allocate buffer directly with size to avoid potential resize issues
                std::vector<char> buffer(static_cast<size_t>(dataSize));
                char *bufferPtr = buffer.data();
                if (bufferPtr == nullptr || buffer.size() != static_cast<size_t>(dataSize))
                {
                    logMessage(rank, size, "[SYNC] ERROR: Failed to allocate buffer for winner block", false);
                    int ack = 0;
                    MPI_Send(&ack, 1, MPI_INT, 0, TAG_BLOCK_WINNER, MPI_COMM_WORLD);
                    auto fullChain = requestFullChainMPI(verbose);
                    if (!fullChain.empty())
                    {
                        result = fullChain.back();
                    }
                    return result;
                }

                MPI_Recv(bufferPtr, dataSize, MPI_CHAR, 0, TAG_BLOCK_WINNER_DATA, MPI_COMM_WORLD, &recvStatus);

                std::string blockData(buffer.data(), dataSize);
                if (blockData.empty())
                {
                    logMessage(rank, size, "[SYNC] ERROR: Received empty block data", false);
                    int ack = 0;
                    MPI_Send(&ack, 1, MPI_INT, 0, TAG_BLOCK_WINNER, MPI_COMM_WORLD);
                    auto fullChain = requestFullChainMPI(verbose);
                    if (!fullChain.empty())
                    {
                        result = fullChain.back();
                    }
                    return result;
                }

                Block receivedBlock = deserializeBlock(blockData);

                // Validate block structure before accessing fields
                if (receivedBlock.previousHash.empty() && receivedBlock.index == 0 && receivedBlock.hash.empty())
                {
                    logMessage(rank, size, "[SYNC] ERROR: Deserialized block appears invalid", false);
                    int ack = 0;
                    MPI_Send(&ack, 1, MPI_INT, 0, TAG_BLOCK_WINNER, MPI_COMM_WORLD);
                    auto fullChain = requestFullChainMPI(verbose);
                    if (!fullChain.empty())
                    {
                        result = fullChain.back();
                    }
                    return result;
                }

                std::string prevHashPreview = prevHash.size() >= 8 ? prevHash.substr(0, 8) : prevHash;
                std::string receivedPrevHashPreview = receivedBlock.previousHash.size() >= 8 ? receivedBlock.previousHash.substr(0, 8) : receivedBlock.previousHash;
                logMessage(rank, size, "[SYNC] Validating winner block (prevHash=" + receivedPrevHashPreview + "..., expected=" + prevHashPreview + "...)", false);

                if (receivedBlock.previousHash == prevHash)
                {
                    result = receivedBlock;
                    int ack = 1;
                    logMessage(rank, size, "[SYNC] Sending ACK to rank 0...", false);
                    MPI_Send(&ack, 1, MPI_INT, 0, TAG_BLOCK_WINNER, MPI_COMM_WORLD);
                    logMessage(rank, size, "[SYNC] ACK sent to rank 0", false);
                }
                else
                {
                    int ack = 0;
                    MPI_Send(&ack, 1, MPI_INT, 0, TAG_BLOCK_WINNER, MPI_COMM_WORLD);
                    if (verbose)
                    {
                        logMessage(rank, size, "[SYNC] Winner block prevHash mismatch - requesting full chain", verbose);
                    }
                    auto fullChain = requestFullChainMPI(verbose);
                    if (!fullChain.empty())
                    {
                        result = fullChain.back();
                    }
                }
            }
            catch (const std::exception &e)
            {
                logMessage(rank, size, "[SYNC] ERROR: Exception receiving winner block: " + std::string(e.what()), false);
                int ack = 0;
                MPI_Send(&ack, 1, MPI_INT, 0, TAG_BLOCK_WINNER, MPI_COMM_WORLD);
                auto fullChain = requestFullChainMPI(verbose);
                if (!fullChain.empty())
                {
                    result = fullChain.back();
                }
                return result;
            }
        }

        return result;
    }
#else
    Block mineBlockMPI(
        int index,
        const std::string &data,
        const std::string &prevHash,
        int difficulty,
        int threadsPerProcess,
        const std::vector<Block> &fullChain,
        bool verbose)
    {

        auto timestamp = std::chrono::system_clock::now();
        std::atomic<bool> found(false);
        std::mutex resultMutex;
        Block result(index, data, timestamp, prevHash, difficulty, 0, false);
        bool resultSet = false;

        auto worker = [&](int threadId, int stride)
        {
            // Use unsigned arithmetic to avoid undefined behavior on overflow
            unsigned int startNonce = static_cast<unsigned int>(threadId);
            unsigned int uNonce = startNonce;
            const unsigned int maxNonce = static_cast<unsigned int>(std::numeric_limits<int>::max());

            while (!found.load(std::memory_order_relaxed))
            {
                // Check for overflow before incrementing
                if (uNonce > maxNonce - static_cast<unsigned int>(stride)) {
                    // Wrapped around - restart from beginning of this thread's range
                    uNonce = startNonce;
                }

                int nonce = static_cast<int>(uNonce);
                Block candidate(index, data, timestamp, prevHash, difficulty, nonce, false);
                std::string hash = candidate.computeHash();

                if (hasLeadingZeros(hash, difficulty))
                {
                    bool expected = false;
                    if (found.compare_exchange_strong(expected, true, std::memory_order_release, std::memory_order_relaxed))
                    {
                        std::lock_guard<std::mutex> lock(resultMutex);
                        if (!resultSet)
                        {
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
        threads.reserve(threadsPerProcess);

        for (int i = 0; i < threadsPerProcess; ++i)
        {
            threads.emplace_back(worker, i, threadsPerProcess);
        }

        for (auto &t : threads)
        {
            t.join();
        }

        return result;
    }

    bool syncSnapshotMPI(const std::string &, int, bool) { return true; }
    void distributeWinnerBlockMPI(const Block &, const std::vector<Block> &, bool) {}
    bool receiveWinnerBlockMPI(const std::string &, Block &, bool) { return false; }
    std::vector<Block> requestFullChainMPI(bool) { return {}; }
    void sendFullChainMPI(const std::vector<Block> &, int, bool) {}
    std::vector<Block> broadcastChainToAllRanksMPI(const std::vector<Block> &chain, bool) { return chain; }
    std::vector<Block> checkForChainUpdateMPI(bool) { return {}; }
#endif
}
