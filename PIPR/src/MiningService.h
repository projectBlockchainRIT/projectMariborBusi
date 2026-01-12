#pragma once

#include <functional>
#include <mutex>
#include <optional>
#include <string>

#include "Blockchain.h"

class MiningService {
public:
    using BlockMinedHandler = std::function<void(const Block&)>;

    explicit MiningService(Blockchain& blockchain, int numThreads = 1, bool useMPI = false);

    std::optional<Block> MineBlock(const std::string& data, bool verbose = false);
    void SetBlockMinedHandler(BlockMinedHandler handler);

private:
    Blockchain& blockchain_;
    std::mutex mineMutex_;
    BlockMinedHandler blockMinedHandler_;
    int numThreads_;
    bool useMPI_;
};

