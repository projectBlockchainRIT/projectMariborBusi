#pragma once

#include <optional>
#include <string>
#include <vector>

#include "Block.h"

std::string SerializeChain(const std::vector<Block>& chain);
std::optional<std::vector<Block>> DeserializeChain(const std::string& jsonText);

