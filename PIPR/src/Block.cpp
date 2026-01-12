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
extern "C" time_t timegm(struct tm* tm);
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

// Pretvori čas v ISO format (UTC)
std::string TimePointToIso(const std::chrono::system_clock::time_point& tp) {
    std::time_t tt = std::chrono::system_clock::to_time_t(tp);
    std::tm tm{};
#ifdef _WIN32
    gmtime_s(&tm, &tt);
#else
    gmtime_r(&tt, &tm);
#endif
    std::ostringstream oss;
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
    // Serializiraj blok za izračun hasha
    std::ostringstream oss;
    oss.imbue(std::locale::classic());
    oss << index << data << TimestampString() << previousHash << difficulty << nonce;
    return oss.str();
}

std::string Block::computeHash() const {
    return Sha256Hash(toStringForHash());
}

std::string Block::CalculateHash() {
    // Stara metoda - izvaja mining (povečuje nonce)
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
        return std::chrono::system_clock::from_time_t(0);
    }
    
    // Pretvori UTC čas
    #ifdef _WIN32
        auto time_c = _mkgmtime(&tm);
    #else
        auto time_c = timegm(&tm);
        if (time_c == -1) {
            time_c = std::mktime(&tm);
        }
    #endif
    
    if (time_c == -1) {
        return std::chrono::system_clock::from_time_t(0);
    }
    
    return std::chrono::system_clock::from_time_t(time_c);
}

