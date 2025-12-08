#include "Blockchain.h"

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

Block Blockchain::CreateGenesisBlock() const {
    return Block(0, "Genesis Block", std::chrono::system_clock::now(), "0",
                 std::max(1, difficulty_));
}

Block Blockchain::GetLatestBlock() const {
    std::scoped_lock lock(chainMutex_);
    return chain_.back();
}

std::vector<Block> Blockchain::GetChain() const {
    std::scoped_lock lock(chainMutex_);
    return chain_;
}

bool Blockchain::AddBlock(const Block& newBlock) {
    std::scoped_lock lock(chainMutex_);
    auto tempChain = chain_;
    tempChain.push_back(newBlock);

    std::string reason;
    if (!ValidateChain(tempChain, &reason)) {
        if (!reason.empty()) {
            std::cout << "AddBlock rejected: " << reason << "\n";
        }
        return false;
    }

    chain_.push_back(newBlock);
    return true;
}

bool Blockchain::ValidateChain(const std::vector<Block>& chainToValidate) const {
    return ValidateChain(chainToValidate, nullptr);
}

bool Blockchain::ReplaceChain(const std::vector<Block>& newChain) {
    std::string reason;
    if (!ValidateChain(newChain, &reason)) {
        if (!reason.empty()) {
            std::cout << "ReplaceChain rejected: " << reason << "\n";
        }
        return false;
    }

    std::scoped_lock lock(chainMutex_);
    if (!IsBetterChain(newChain)) {
        return false;
    }

    chain_ = newChain;
    return true;
}

int Blockchain::GetAdjustedDifficulty() const {
    std::scoped_lock lock(chainMutex_);

    if (chain_.empty()) {
        return std::max(1, difficulty_);
    }

    if ((chain_.size() % difficultyAdjustmentInterval_) != 0) {
        return std::max(1, chain_.back().difficulty);
    }

    const auto& prevAdjustmentBlock =
        chain_[chain_.size() - difficultyAdjustmentInterval_];
    const auto expectedTime = std::chrono::seconds(
        blockGenerationInterval_ * difficultyAdjustmentInterval_);
    const auto timeTaken = chain_.back().timestamp - prevAdjustmentBlock.timestamp;

    if (timeTaken < expectedTime / 2) {
        return std::max(1, chain_.back().difficulty + 1);
    }

    if (timeTaken > expectedTime * 2) {
        return std::max(1, chain_.back().difficulty - 1);
    }

    return std::max(1, chain_.back().difficulty);
}

double Blockchain::CalculateCumulativeDifficulty(
    const std::vector<Block>& chain) {
    double sum = 0.0;
    for (const auto& block : chain) {
        sum += std::pow(2.0, block.difficulty);
    }
    return sum;
}

bool Blockchain::IsBetterChain(const std::vector<Block>& candidate) const {
    auto currentDifficulty = CalculateCumulativeDifficulty(chain_);
    auto candidateDifficulty = CalculateCumulativeDifficulty(candidate);

    if (candidateDifficulty > currentDifficulty) {
        return true;
    }

    if (std::abs(candidateDifficulty - currentDifficulty) < 1e-9 &&
        candidate.size() > chain_.size()) {
        return true;
    }

    return false;
}

bool Blockchain::ValidateChain(const std::vector<Block>& chainToValidate,
                               std::string* reason) const {
    if (chainToValidate.empty()) {
        if (reason) *reason = "Chain is empty";
        return false;
    }

    const auto maxGap = std::chrono::minutes(60);  // allow up to 1h gap

    for (std::size_t i = 1; i < chainToValidate.size(); ++i) {
        const auto& current = chainToValidate[i];
        const auto& previous = chainToValidate[i - 1];

        if (current.index != previous.index + 1) {
            if (reason) *reason = "Invalid block index sequence";
            return false;
        }

        if (current.previousHash != previous.hash) {
            if (reason) *reason = "Invalid previous hash reference";
            return false;
        }

        if (current.difficulty < 1) {
            if (reason) *reason = "Difficulty less than 1";
            return false;
        }

        std::ostringstream payload;
        payload << current.index << current.TimestampString() << current.data
                << current.previousHash << current.difficulty << current.nonce;
        auto calculated = Block::Sha256Hash(payload.str());
        if (current.hash != calculated) {
            if (reason) *reason = "Invalid block hash";
            return false;
        }

        auto now = std::chrono::system_clock::now();
        if (current.timestamp > now + std::chrono::minutes(1)) {
            if (reason) *reason = "Block timestamp is in the future";
            return false;
        }

        if (current.timestamp < previous.timestamp) {
            if (reason) *reason = "Block timestamp earlier than previous";
            return false;
        }

        if (current.timestamp > previous.timestamp + maxGap) {
            if (reason) *reason = "Block timestamp too far after previous";
            return false;
        }
    }

    return true;
}

