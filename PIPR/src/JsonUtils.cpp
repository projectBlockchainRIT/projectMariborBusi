#include "JsonUtils.h"

#include <regex>
#include <sstream>

namespace {
std::string Escape(const std::string& input) {
    std::string out;
    out.reserve(input.size());
    for (char c : input) {
        if (c == '"' || c == '\\') {
            out.push_back('\\');
        }
        out.push_back(c);
    }
    return out;
}

std::optional<std::string> ExtractString(const std::string& source,
                                         const std::string& key) {
    std::regex pattern("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
    std::smatch match;
    if (std::regex_search(source, match, pattern) && match.size() > 1) {
        return match[1].str();
    }
    return std::nullopt;
}

std::optional<int> ExtractInt(const std::string& source,
                              const std::string& key) {
    std::regex pattern("\"" + key + "\"\\s*:\\s*(-?\\d+)");
    std::smatch match;
    if (std::regex_search(source, match, pattern) && match.size() > 1) {
        return std::stoi(match[1].str());
    }
    return std::nullopt;
}
}  // namespace

std::string SerializeChain(const std::vector<Block>& chain) {
    std::ostringstream oss;
    oss << "[";
    for (std::size_t i = 0; i < chain.size(); ++i) {
        const auto& b = chain[i];
        if (i > 0) {
            oss << ",";
        }
        oss << "{";
        oss << "\"Index\":" << b.index << ",";
        oss << "\"Data\":\"" << Escape(b.data) << "\",";
        oss << "\"Timestamp\":\"" << b.TimestampString() << "\",";
        oss << "\"Hash\":\"" << b.hash << "\",";
        oss << "\"PreviousHash\":\"" << b.previousHash << "\",";
        oss << "\"Difficulty\":" << b.difficulty << ",";
        oss << "\"Nonce\":" << b.nonce;
        oss << "}";
    }
    oss << "]";
    return oss.str();
}

std::optional<std::vector<Block>> DeserializeChain(const std::string& jsonText) {
    std::vector<Block> blocks;
    std::size_t pos = 0;
    while (true) {
        auto start = jsonText.find('{', pos);
        if (start == std::string::npos) break;
        auto end = jsonText.find('}', start);
        if (end == std::string::npos) break;

        std::string obj = jsonText.substr(start, end - start + 1);
        pos = end + 1;

        auto idxOpt = ExtractInt(obj, "Index");
        auto dataOpt = ExtractString(obj, "Data");
        auto tsOpt = ExtractString(obj, "Timestamp");
        auto hashOpt = ExtractString(obj, "Hash");
        auto prevHashOpt = ExtractString(obj, "PreviousHash");
        auto diffOpt = ExtractInt(obj, "Difficulty");
        auto nonceOpt = ExtractInt(obj, "Nonce");

        if (!idxOpt || !dataOpt || !tsOpt || !hashOpt || !prevHashOpt ||
            !diffOpt || !nonceOpt) {
            return std::nullopt;
        }

        // Parse timestamp once to avoid redundant parsing
        auto parsedTimestamp = Block::ParseTimestamp(*tsOpt);
        Block b(*idxOpt, *dataOpt, parsedTimestamp, *prevHashOpt, *diffOpt, *nonceOpt, false);
        b.difficulty = *diffOpt;
        b.hash = *hashOpt;
        b.nonce = *nonceOpt;
        b.timestamp = parsedTimestamp;  // Use already parsed timestamp

        blocks.push_back(std::move(b));
    }

    if (blocks.empty()) {
        return std::nullopt;
    }
    return blocks;
}

