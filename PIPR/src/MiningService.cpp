#include "MiningService.h"

#include <chrono>
#include <optional>
#include <string>

MiningService::MiningService(Blockchain &blockchain)
    : blockchain_(blockchain) {}

std::optional<Block> MiningService::MineBlock(const std::string &data)
{
    std::scoped_lock lock(mineMutex_);

    auto latest = blockchain_.GetLatestBlock();
    auto difficulty = std::max(1, blockchain_.GetAdjustedDifficulty());

    Block newBlock(latest.index + 1, data, std::chrono::system_clock::now(),
                   latest.hash, difficulty);

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
