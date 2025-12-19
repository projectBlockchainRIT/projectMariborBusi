#include "MiningService.h"

#include <chrono>
#include <optional>
#include <string>

#include "Mining.h"

MiningService::MiningService(Blockchain &blockchain)
    : blockchain_(blockchain) {}

std::optional<Block> MiningService::MineBlock(const std::string &data)
{
    std::scoped_lock lock(mineMutex_);

    auto latest = blockchain_.GetLatestBlock();
    auto difficulty = std::max(1, blockchain_.GetAdjustedDifficulty());

    // Use the new Mining::mineBlock() function for sequential mining
    Block newBlock = Mining::mineBlock(
        latest.index + 1,
        data,
        latest.hash,
        difficulty
    );

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
