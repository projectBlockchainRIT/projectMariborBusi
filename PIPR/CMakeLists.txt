cmake_minimum_required(VERSION 3.20)
project(nal5_cplus LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find MPI (optional - project works without it)
find_package(MPI QUIET)

add_executable(nal5_cplus
    src/main.cpp
    src/Block.cpp
    src/Blockchain.cpp
    src/MiningService.cpp
    src/Mining.cpp
    src/DistributedMining.cpp
    src/Crypto.cpp
    src/PeerNetwork.cpp
    src/JsonUtils.cpp
)

target_include_directories(nal5_cplus PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/src)

# Link MPI if available
if(MPI_FOUND)
    target_link_libraries(nal5_cplus PRIVATE MPI::MPI_CXX)
    # Define USE_MPI preprocessor macro
    target_compile_definitions(nal5_cplus PRIVATE USE_MPI)
    message(STATUS "MPI found - distributed mining enabled")
else()
    message(STATUS "MPI not found - distributed mining disabled (project will work with single process)")
endif()

if(WIN32)
    target_link_libraries(nal5_cplus PRIVATE ws2_32)
endif()

