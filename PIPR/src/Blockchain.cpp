#include "Blockchain.h"
#include "AppSettings.h"

#include <algorithm>
#include <cmath>
#include <chrono>
#include <iostream>
#include <mutex>
#include <sstream>
#include <string>

Blockchain::Blockchain(int difficulty,
                       int blockGenerationInterval,
                       int difficultyAdjustmentInterval)
    : difficulty_(std::max(1, difficulty)),
      blockGenerationInterval_(blockGenerationInterval),
      difficultyAdjustmentInterval_(difficultyAdjustmentInterval),
      chain_({CreateGenesisBlock()}) {}

Block Blockchain::CreateGenesisBlock() const
{
    // Ustvari začetni blok z fiksnimi vrednostmi
    auto genesisTime = std::chrono::system_clock::from_time_t(0);
    int genDifficulty = AppSettings::DefaultDifficulty;
    int genNonce = 0;

    Block genesis(0, "Genesis Block", genesisTime, "0", genDifficulty, genNonce, false);

    genesis.hash = genesis.computeHash();

    return genesis;
}

Block Blockchain::GetLatestBlock() const
{
    std::scoped_lock lock(chainMutex_);
    return chain_.back();
}

std::vector<Block> Blockchain::GetChain() const
{
    std::scoped_lock lock(chainMutex_);
    return chain_;
}

bool Blockchain::AddBlock(const Block &newBlock)
{
    std::scoped_lock lock(chainMutex_);

    if (chain_.empty())
    {
        return false;
    }

    const Block &previous = chain_.back();
    if (!isValidNewBlock(newBlock, previous))
    {
        return false;
    }

    chain_.push_back(newBlock);
    return true;
}

bool Blockchain::isValidNewBlock(const Block &current, const Block &previous) const
{
    if (current.index != previous.index + 1)
    {
        return false;
    }

    if (current.previousHash != previous.hash)
    {
        return false;
    }

    std::string computedHash = current.computeHash();
    if (current.hash != computedHash)
    {
        return false;
    }

    if (current.difficulty < 1)
    {
        return false;
    }

    int leadingZeros = 0;
    for (size_t i = 0; i < current.hash.size() && i < static_cast<size_t>(current.difficulty); ++i)
    {
        if (current.hash[i] == '0')
        {
            ++leadingZeros;
        }
        else
        {
            break;
        }
    }

    if (leadingZeros < current.difficulty)
    {
        return false;
    }

    // Validacija časovnih značk
    auto now = std::chrono::system_clock::now();
    auto timeDiff = std::chrono::duration_cast<std::chrono::minutes>(current.timestamp - now).count();
    if (timeDiff > 1)
    {
        return false; // Časovna značka je več kot 1 minuto v prihodnosti
    }

    auto prevTimeDiff = std::chrono::duration_cast<std::chrono::minutes>(previous.timestamp - current.timestamp).count();
    if (prevTimeDiff > 1)
    {
        return false; // Časovna značka je več kot 1 minuto manjša od prejšnjega bloka
    }

    return true;
}

bool Blockchain::isValidChain() const
{
    std::scoped_lock lock(chainMutex_);
    return ValidateChain(chain_);
}

bool Blockchain::ValidateChain(const std::vector<Block> &chainToValidate) const
{
    return ValidateChain(chainToValidate, nullptr);
}

bool Blockchain::ReplaceChain(const std::vector<Block> &newChain)
{
    std::string reason;
    if (!ValidateChain(newChain, &reason))
    {
        if (!reason.empty())
        {
            std::cout << "ReplaceChain rejected: " << reason << "\n";
        }
        return false;
    }

    std::scoped_lock lock(chainMutex_);

    if (newChain.size() <= chain_.size())
    {
        return false;
    }

    if (!IsBetterChain(newChain))
    {
        return false;
    }

    chain_ = newChain;
    return true;
}

int Blockchain::GetAdjustedDifficulty() const
{
    std::scoped_lock lock(chainMutex_);

    if (chain_.empty())
    {
        return std::max(1, difficulty_);
    }

    if ((chain_.size() % difficultyAdjustmentInterval_) != 0)
    {
        return std::max(1, chain_.back().difficulty);
    }

    const auto &prevAdjustmentBlock =
        chain_[chain_.size() - difficultyAdjustmentInterval_];
    const auto expectedTime = std::chrono::seconds(
        blockGenerationInterval_ * difficultyAdjustmentInterval_);
    const auto timeTaken = chain_.back().timestamp - prevAdjustmentBlock.timestamp;

    if (timeTaken < expectedTime / 2)
    {
        return std::max(1, chain_.back().difficulty + 1);
    }

    if (timeTaken > expectedTime * 2)
    {
        return std::max(1, chain_.back().difficulty - 1);
    }

    return std::max(1, chain_.back().difficulty);
}

double Blockchain::CalculateCumulativeDifficulty(
    const std::vector<Block> &chain)
{
    double sum = 0.0;
    for (const auto &block : chain)
    {
        sum += std::pow(2.0, block.difficulty);
    }
    return sum;
}

bool Blockchain::IsBetterChain(const std::vector<Block> &candidate) const
{
    auto currentDifficulty = CalculateCumulativeDifficulty(chain_);
    auto candidateDifficulty = CalculateCumulativeDifficulty(candidate);

    if (candidateDifficulty > currentDifficulty)
    {
        return true;
    }

    if (std::abs(candidateDifficulty - currentDifficulty) < 1e-9 &&
        candidate.size() > chain_.size())
    {
        return true;
    }

    return false;
}

bool Blockchain::ValidateChain(const std::vector<Block> &chainToValidate,
                               std::string *reason) const
{
    if (chainToValidate.empty())
    {
        if (reason)
            *reason = "Chain is empty";
        return false;
    }

    const auto &genesis = chainToValidate[0];

    if (genesis.index != 0)
    {
        if (reason)
            *reason = "Genesis block must have index 0";
        return false;
    }
    if (genesis.previousHash != "0")
    {
        if (reason)
            *reason = "Genesis block previousHash must be '0'";
        return false;
    }
    if (genesis.data != "Genesis Block")
    {
        if (reason)
            *reason = "Genesis block data must be 'Genesis Block'";
        return false;
    }
    if (genesis.nonce != 0)
    {
        if (reason)
            *reason = "Genesis block nonce must be 0";
        return false;
    }

    auto expectedTime = std::chrono::system_clock::from_time_t(0);
    auto timeDiff = std::chrono::duration_cast<std::chrono::seconds>(
                        genesis.timestamp - expectedTime)
                        .count();
    if (std::abs(timeDiff) > 1)
    {
        if (reason)
            *reason = "Genesis block timestamp must be epoch 0 (1970-01-01T00:00:00)";
        return false;
    }

    std::string genesisComputed = genesis.computeHash();
    if (genesis.hash != genesisComputed)
    {
        if (reason)
            *reason = "Genesis block hash mismatch";
        return false;
    }

    for (std::size_t i = 1; i < chainToValidate.size(); ++i)
    {
        const auto &current = chainToValidate[i];
        const auto &previous = chainToValidate[i - 1];

        if (!isValidNewBlock(current, previous))
        {
            if (reason)
            {
                if (current.index != previous.index + 1)
                {
                    *reason = "Invalid block index sequence";
                }
                else if (current.previousHash != previous.hash)
                {
                    *reason = "Invalid previous hash reference";
                }
                else if (current.hash != current.computeHash())
                {
                    *reason = "Invalid block hash";
                }
                else
                {
                    *reason = "Hash does not meet difficulty requirement";
                }
            }
            return false;
        }
    }

    return true;
}
