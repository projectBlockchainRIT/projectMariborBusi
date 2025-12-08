cmake_minimum_required(VERSION 3.20)
project(nal5_cplus LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_executable(nal5_cplus
    src/main.cpp
    src/Block.cpp
    src/Blockchain.cpp
    src/MiningService.cpp
    src/PeerNetwork.cpp
    src/JsonUtils.cpp
)

target_include_directories(nal5_cplus PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/src)

if(WIN32)
    target_link_libraries(nal5_cplus ws2_32)
endif()

