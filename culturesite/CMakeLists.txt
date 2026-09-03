cmake_minimum_required(VERSION 4.2)
project(culturesite)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -static")

add_executable(culturesite main.cpp)

if (WIN32)
    target_link_libraries(culturesite PRIVATE ws2_32)
endif()