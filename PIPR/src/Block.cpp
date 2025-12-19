#include "Block.h"

#include <chrono>
#include <ctime>
#include <iomanip>
#include <locale>
#include <sstream>
#include <string>

#ifdef _WIN32
#include <time.h>
#else
#include <time.h>
extern "C" time_t timegm(struct tm* tm);  // May not be available on all systems
#endif

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

// Deterministic timestamp formatting - uses UTC to avoid locale issues
std::string TimePointToIso(const std::chrono::system_clock::time_point& tp) {
    std::time_t tt = std::chrono::system_clock::to_time_t(tp);
    std::tm tm{};
#ifdef _WIN32
    gmtime_s(&tm, &tt);
#else
    gmtime_r(&tt, &tm);
#endif
    std::ostringstream oss;
    // Use C locale for deterministic formatting
    oss.imbue(std::locale::classic());
    oss << std::put_time(&tm, "%Y-%m-%dT%H:%M:%S");
    return oss.str();
}
}  // namespace

Block::Block(int idx,
             std::string blockData,
             std::chrono::system_clock::time_point time,
             std::string prevHash,
             int diff,
             int nonceValue,
             bool computeHashNow)
    : index(idx),
      data(std::move(blockData)),
      timestamp(time),
      hash(""),
      previousHash(std::move(prevHash)),
      difficulty(diff),
      nonce(nonceValue) {
    if (computeHashNow) {
        hash = computeHash();
    }
}

std::string Block::toStringForHash() const {
    // Deterministic serialization: index + data + timestamp + prevHash + difficulty + nonce
    // Use C locale to ensure consistent formatting
    std::ostringstream oss;
    oss.imbue(std::locale::classic());
    oss << index << data << TimestampString() << previousHash << difficulty << nonce;
    return oss.str();
}

std::string Block::computeHash() const {
    return Sha256Hash(toStringForHash());
}

std::string Block::CalculateHash() {
    // Legacy method - kept for backward compatibility
    // This method does mining (increments nonce), use computeHash() for just computing hash
    std::string computed;
    do {
        computed = computeHash();
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
    iss.imbue(std::locale::classic());
    iss >> std::get_time(&tm, "%Y-%m-%dT%H:%M:%S");
    if (iss.fail()) {
        // Fallback to epoch 0 for genesis block compatibility
        return std::chrono::system_clock::from_time_t(0);
    }
    
    // Parse as UTC time (not local time)
    // mktime interprets tm as local time, so we need to use timegm or manual calculation
    // For portability, we'll use a workaround: set timezone to UTC temporarily
    #ifdef _WIN32
        // Windows: _mkgmtime converts UTC tm to time_t
        auto time_c = _mkgmtime(&tm);
    #else
        // Unix: timegm converts UTC tm to time_t (if available)
        // Otherwise use mktime with UTC timezone
        auto time_c = timegm(&tm);
        if (time_c == -1) {
            // Fallback: treat as UTC by using mktime and adjusting
            time_c = std::mktime(&tm);
            // This is not perfect but better than nothing
        }
    #endif
    
    if (time_c == -1) {
        // If conversion fails, return epoch 0
        return std::chrono::system_clock::from_time_t(0);
    }
    
    return std::chrono::system_clock::from_time_t(time_c);
}

