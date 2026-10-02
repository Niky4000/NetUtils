cmake_minimum_required(VERSION 4.2)
project(culturesite)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -static")

# 0. ВКЛЮЧАЕМ ПОИСК ТОЛЬКО СТАТИЧЕСКИХ БИБЛИОТЕК OpenSSL
set(OPENSSL_USE_STATIC_LIBS TRUE)

# 1. Находим пакет OpenSSL в системе
find_package(OpenSSL REQUIRED)

add_executable(culturesite main.cpp)

# 2. Линкуем OpenSSL к вашему исполняемому файлу
# OpenSSL::SSL автоматически подтянет и OpenSSL::Crypto, и нужные пути к заголовочным файлам
target_link_libraries(culturesite PRIVATE OpenSSL::SSL)

if (WIN32)
    # На Windows для работы сокетов и OpenSSL нужна библиотека ws2_32,
    # а также crypt32 (используется OpenSSL для работы с системными сертификатами Windows)
    target_link_libraries(culturesite PRIVATE ws2_32 crypt32)
endif()