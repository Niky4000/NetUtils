cmake_minimum_required(VERSION 4.2)
project(culturesite)

set(CMAKE_CXX_STANDARD 26)
#set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -static-libstdc++ -static-libgcc")
set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -static")

add_executable(culturesite main.cpp)
