//
// Created by me on 14/09/2026.
//
#include "SocketListerner.cpp"
#include <sstream>
#include <fstream>

class Page : public SocketListerner {
protected:
    std::string createResponse(std::string request) override {
        std::filesystem::path baseDir = getPathFromRequest(request, "GET ");
        std::string type = getType(baseDir);
        std::string data = readFile(baseDir);
        // byte[] content = handle(FileUtils.readAllBytesFromFile(baseDir), type);
        // std::string data = "<html><head><title>My Server</title></head><body><h1>Hello, World!!!</h1><h2>Hello!!!</h2></body></html>";
        std::stringstream ss;
        ss << "HTTP/1.1 200\n"
                << "content-length: " << data.length() << "\n"
                << "cache-control: no-cache\n"
                << "content-type: text/html\n"
                << "connection: close\n\n";
        ss << data;
        return ss.str();
    }

private:
    std::string readFile(const std::string &filepath) {
        std::filesystem::path currentFile = std::filesystem::path{filepath};
        if (is_directory(currentFile) || !exists(currentFile)) {
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

public:
    Page();

    ~Page();
};

Page::Page() {
}

Page::~Page() {
}
