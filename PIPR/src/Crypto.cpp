#include "Crypto.h"
#include "Sha256.h"

namespace Crypto {
    std::string sha256(const std::string& input) {
        return ::sha256(input);
    }
}

