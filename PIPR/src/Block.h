#pragma once

#include <chrono>
#include <string>

class Block
{
public:
    int index{0};
    std::string data{};
    std::chrono::system_clock::time_point timestamp{};
    std::string hash{};
    std::string previousHash{};
    int difficulty{0};
    int nonce{0};

    Block(int idx,
          std::string blockData,
          std::chrono::system_clock::time_point time,
          std::string prevHash,
          int diff,
          int nonceValue = 0,
          bool computeHashNow = false);

    // Deterministic serialization for hashing
    std::string toStringForHash() const;

    // Compute hash without mining (uses current nonce)
    std::string computeHash() const;

    // Legacy method - kept for compatibility but should use computeHash()
    std::string CalculateHash();

    static std::string Sha256Hash(const std::string &input);

    std::string TimestampString() const;
    static std::chrono::system_clock::time_point ParseTimestamp(
        const std::string &timestampStr);
};
