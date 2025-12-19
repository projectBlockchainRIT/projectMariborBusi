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
    // Genesis block: STATIC and IDENTICAL on all nodes
    // Fixed timestamp (epoch 0 = 1970-01-01T00:00:00 UTC)
    // Fixed nonce = 0
    // Fixed difficulty = 0 (genesis doesn't need mining/validation)
    // Fixed data = "Genesis Block"
    // Fixed prevHash = "0"
    
    auto genesisTime = std::chrono::system_clock::from_time_t(0); // Epoch 0
    int genDifficulty = 0; // Genesis doesn't need difficulty validation
    int genNonce = 0; // Fixed nonce for genesis
    
    Block genesis(0, "Genesis Block", genesisTime, "0", genDifficulty, genNonce, false);
    
    // Compute hash with fixed values (no mining needed)
    genesis.hash = genesis.computeHash();
    
    return genesis;
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
    
    if (chain_.empty()) {
        // Should not happen - genesis block is created in constructor
        return false;
    }
    
    const Block& previous = chain_.back();
    if (!isValidNewBlock(newBlock, previous)) {
        return false;
    }

    chain_.push_back(newBlock);
    return true;
}

bool Blockchain::isValidNewBlock(const Block& current, const Block& previous) const {
    // Check index sequence
    if (current.index != previous.index + 1) {
        return false;
    }
    
    // Check previous hash reference
    if (current.previousHash != previous.hash) {
        return false;
    }
    
    // Check that computed hash matches stored hash
    std::string computedHash = current.computeHash();
    if (current.hash != computedHash) {
        return false;
    }
    
    // Check difficulty (hash must have correct number of leading zeros)
    if (current.difficulty < 1) {
        return false;
    }
    
    int leadingZeros = 0;
    for (size_t i = 0; i < current.hash.size() && i < static_cast<size_t>(current.difficulty); ++i) {
        if (current.hash[i] == '0') {
            ++leadingZeros;
        } else {
            break;
        }
    }
    
    if (leadingZeros < current.difficulty) {
        return false;
    }
    
    return true;
}

bool Blockchain::isValidChain() const {
    std::scoped_lock lock(chainMutex_);
    return ValidateChain(chain_);
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

    // Validate genesis block - must be STATIC and IDENTICAL
    const auto& genesis = chainToValidate[0];
    
    // Basic structure validation
    if (genesis.index != 0) {
        if (reason) *reason = "Genesis block must have index 0";
        return false;
    }
    if (genesis.previousHash != "0") {
        if (reason) *reason = "Genesis block previousHash must be '0'";
        return false;
    }
    if (genesis.data != "Genesis Block") {
        if (reason) *reason = "Genesis block data must be 'Genesis Block'";
        return false;
    }
    if (genesis.nonce != 0) {
        if (reason) *reason = "Genesis block nonce must be 0";
        return false;
    }
    
    // Validate genesis block timestamp (must be epoch 0)
    auto expectedTime = std::chrono::system_clock::from_time_t(0);
    auto timeDiff = std::chrono::duration_cast<std::chrono::seconds>(
        genesis.timestamp - expectedTime).count();
    if (std::abs(timeDiff) > 1) { // Allow 1 second tolerance for time_t conversion
        if (reason) *reason = "Genesis block timestamp must be epoch 0 (1970-01-01T00:00:00)";
        return false;
    }
    
    // Validate genesis block hash - must match computed hash with fixed values
    std::string genesisComputed = genesis.computeHash();
    if (genesis.hash != genesisComputed) {
        if (reason) *reason = "Genesis block hash mismatch";
        return false;
    }

    // Validate each subsequent block
    for (std::size_t i = 1; i < chainToValidate.size(); ++i) {
        const auto& current = chainToValidate[i];
        const auto& previous = chainToValidate[i - 1];

        // Use isValidNewBlock for consistency
        if (!isValidNewBlock(current, previous)) {
            if (reason) {
                // Determine specific reason
                if (current.index != previous.index + 1) {
                    *reason = "Invalid block index sequence";
                } else if (current.previousHash != previous.hash) {
                    *reason = "Invalid previous hash reference";
                } else if (current.hash != current.computeHash()) {
                    *reason = "Invalid block hash";
                } else {
                    *reason = "Hash does not meet difficulty requirement";
                }
            }
            return false;
        }
    }

    return true;
}

