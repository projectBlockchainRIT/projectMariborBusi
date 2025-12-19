#pragma once

#include <mutex>
#include <vector>

#include "Block.h"

class Blockchain {
public:
    Blockchain(int difficulty = 4,
               int blockGenerationInterval = 10,
               int difficultyAdjustmentInterval = 10);

    Block GetLatestBlock() const;
    std::vector<Block> GetChain() const;

    bool AddBlock(const Block& newBlock);
    bool isValidNewBlock(const Block& current, const Block& previous) const;
    bool isValidChain() const;
    bool ValidateChain(const std::vector<Block>& chainToValidate) const;
    bool ReplaceChain(const std::vector<Block>& newChain);
    int GetAdjustedDifficulty() const;

private:
    bool IsBetterChain(const std::vector<Block>& candidate) const;
    bool ValidateChain(const std::vector<Block>& chainToValidate,
                       std::string* reason) const;
    Block CreateGenesisBlock() const;
    static double CalculateCumulativeDifficulty(const std::vector<Block>& chain);

    mutable std::mutex chainMutex_;
    std::vector<Block> chain_;
    int difficulty_;
    int blockGenerationInterval_;
    int difficultyAdjustmentInterval_;
};

