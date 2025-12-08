#include "Block.h"

#include <chrono>
#include <ctime>
#include <iomanip>
#include <sstream>
#include <string>

#include "Sha256.h"

namespace {
bool HasLeadingZeros(const std::string& hash, int difficulty) {
    if (static_cast<int>(hash.size()) < difficulty) {
        return false;
    }
    for (int i = 0; i < difficulty; ++i) {
        if (hash[i] != '0') {
            return false;
        }
    }
    return true;
}

std::string TimePointToIso(const std::chrono::system_clock::time_point& tp) {
    std::time_t tt = std::chrono::system_clock::to_time_t(tp);
    std::tm tm{};
#ifdef _WIN32
    localtime_s(&tm, &tt);
#else
    localtime_r(&tt, &tm);
#endif
    std::ostringstream oss;
    oss << std::put_time(&tm, "%Y-%m-%dT%H:%M:%S");
    return oss.str();
}
}  // namespace

Block::Block(int idx,
             std::string blockData,
             std::chrono::system_clock::time_point time,
             std::string prevHash,
             int diff,
             bool mine)
    : index(idx),
      data(std::move(blockData)),
      timestamp(time),
      hash(""),
      previousHash(std::move(prevHash)),
      difficulty(diff),
      nonce(0) {
    if (mine) {
        hash = CalculateHash();
    }
}

std::string Block::CalculateHash() {
    std::string computed;
    do {
        std::ostringstream payload;
        payload << index << TimestampString() << data << previousHash
                << difficulty << nonce;
        computed = Sha256Hash(payload.str());
        if (!HasLeadingZeros(computed, difficulty)) {
            ++nonce;
        }
    } while (!HasLeadingZeros(computed, difficulty));

    hash = computed;
    return hash;
}

std::string Block::Sha256Hash(const std::string& input) {
    return sha256(input);
}

std::string Block::TimestampString() const {
    return TimePointToIso(timestamp);
}

std::chrono::system_clock::time_point Block::ParseTimestamp(
    const std::string& timestampStr) {
    std::tm tm{};
    std::istringstream iss(timestampStr);
    iss >> std::get_time(&tm, "%Y-%m-%dT%H:%M:%S");
    if (iss.fail()) {
        // Fallback to now if parsing fails.
        return std::chrono::system_clock::now();
    }
    auto time_c = std::mktime(&tm);
    return std::chrono::system_clock::from_time_t(time_c);
}

