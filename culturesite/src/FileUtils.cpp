//
// Created by me on 17/09/2026.
//
#include <iostream>
#include <fstream>
#include <filesystem>
#include <ranges>
#include <string_view>
#include <algorithm>
#include <unordered_map>
#include <vector>

class FileUtils {
private:
#ifdef _WIN32
    std::string s = "\\";
#else
    std::string s = "/";
#endif

    std::string CONFIGS_FILE = "configs.properties";
    std::string ENV_FILE = "env.properties";
    std::unordered_map<std::string, std::string> configMap;
    std::unordered_map<std::string, std::string> envMap;

    void init(std::string configFileName, std::unordered_map<std::string, std::string> &configMapPtr) {
        std::string selfPath = std::filesystem::current_path().c_str();
        std::filesystem::path config = std::filesystem::path{selfPath + s + configFileName};
        long count = std::count(selfPath.begin(), selfPath.end(), s.at(0));
        while (!std::filesystem::exists(config) && count > 1L) {
            std::filesystem::path parentPath = std::filesystem::absolute(selfPath).parent_path();
            selfPath = parentPath.c_str();
            config = std::filesystem::path{selfPath + s + configFileName};
            count = std::count(selfPath.begin(), selfPath.end(), s.at(0));
        }
        if (std::filesystem::exists(config)) {
            std::string configStr = readFile(config.c_str());
            for (const auto row: std::views::split(configStr, '\n')) {
                int index = 0;
                std::string key;
                std::string value;
                for (const auto word: std::views::split(row, '=')) {
                    if (index++ == 0) {
                        key = std::string_view(word.data(), word.size());
                    } else {
                        value = std::string_view(word.data(), word.size());
                    }
                }
                if (key.length() > 0 && value.length() > 0) {
                    configMapPtr.insert(std::make_pair(key, value));
                }
            }
        } else {
            std::cout << "No config file!" << std::endl;
        }

        // configFile = new File(getPathToJar().getParent() + s + CONFIGS_FILE);
        // if (!configFile.exists()) {
        //     configFile = new File(getPathToJar().getParentFile().getParent() + s + CONFIGS_FILE);
        // }
        // envFile = new File(getPathToJar().getParent() + s + ENV_FILE);
        // if (!envFile.exists()) {
        //     envFile = new File(getPathToJar().getParentFile().getParent() + s + ENV_FILE);
        // }
    }

public:
    std::string getConfig(const std::string &conf) const {
        // Добавили const для ссылок и метода
        auto it = configMap.find(conf);
        if (it != configMap.end()) {
            return it->second; // Ключ найден, возвращаем значение
        }
        return ""; // Ключ не найден, возвращаем дефолтное значение (или бросаем exception)
    }

    std::string getEnvConfig(const std::string &conf) const {
        // Добавили const для ссылок и метода
        auto it = envMap.find(conf);
        if (it != envMap.end()) {
            return it->second; // Ключ найден, возвращаем значение
        }
        return ""; // Ключ не найден, возвращаем дефолтное значение (или бросаем exception)
    }

    std::unordered_map<std::string, std::string> getEnvMap() const {
        return envMap;
    }

    std::string readFile(const std::string &filepath) {
        std::filesystem::path currentFile = std::filesystem::path{filepath};
        if (std::filesystem::is_directory(currentFile) || !std::filesystem::exists(currentFile)) {
            return "";
        }
        std::ifstream file(filepath, std::ios::binary); // Binary mode preserves exact size
        if (!file.is_open()) return "";

        // Determine file size and pre-allocate string memory
        auto size = std::filesystem::file_size(filepath);
        std::string content(size, '\0');

        // Read directly into the contiguous string memory block
        file.read(&content[0], size);
        return content;
    }

    std::vector<char> readAllChars(const std::string& filename) {
        // Open at the end of the file (ios::ate) to find the file size immediately
        std::ifstream file(filename, std::ios::binary | std::ios::ate);
        if (!file.is_open()) {
            throw std::runtime_error("Failed to open file: " + filename);
        }
        if (!std::filesystem::is_regular_file(filename)) {
            throw std::runtime_error("Not a regular file: " + filename);
        }
        // Get the current position (which is the file size)
        std::streamsize size = file.tellg();
        if (size < 0) {
            throw std::runtime_error("Failed to determine file size: " + filename);
        }
        // Seek back to the beginning of the file to start reading
        file.seekg(0, std::ios::beg);
        // Allocate a vector large enough to hold all bytes
        std::vector<char> buffer(size);
        // Read all bytes directly into the vector's memory block
        if (!file.read(buffer.data(), size)) {
            throw std::runtime_error("Error reading file: " + filename);
        }
        return buffer;
    }

    std::vector<std::byte> readAllBytes(const std::string& filepath) {
        // Open the file in binary mode and position the stream pointer at the end (ios::ate)
        std::ifstream file(filepath, std::ios::binary | std::ios::ate);
        if (!file.is_open()) {
            throw std::runtime_error("Failed to open file: " + filepath);
        }
        // Get the current position (which is the file size) and reset pointer to the beginning
        std::streamsize size = file.tellg();
        file.seekg(0, std::ios::beg);
        // Allocate the vector to the exact size needed
        std::vector<std::byte> buffer(size);
        // Read the contents directly into the vector buffer
        if (size > 0) {
            file.read(reinterpret_cast<char*>(buffer.data()), size);
        }
        return buffer;
    }

    FileUtils();

    ~FileUtils();
};

FileUtils::FileUtils() {
    init(CONFIGS_FILE, configMap);
    init(ENV_FILE, envMap);
}

FileUtils::~FileUtils() {
    // std::cout << "~FileUtils() destructor!" << std::endl;
}
