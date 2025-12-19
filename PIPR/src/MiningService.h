#pragma once

#include <functional>
#include <mutex>
#include <optional>
#include <string>

#include "Blockchain.h"

class MiningService {
public:
    using BlockMinedHandler = std::function<void(const Block&)>;

    explicit MiningService(Blockchain& blockchain, int numThreads = 1);

    std::optional<Block> MineBlock(const std::string& data);
    void SetBlockMinedHandler(BlockMinedHandler handler);

private:
    Blockchain& blockchain_;
    std::mutex mineMutex_;
    BlockMinedHandler blockMinedHandler_;
    int numThreads_;
};

