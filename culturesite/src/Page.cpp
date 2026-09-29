//
// Created by me on 14/09/2026.
//
#include "SocketListerner.cpp"

class Page : public SocketListerner {
protected:
    std::vector<char> createResponse(std::string request) override {
        std::filesystem::path baseDir = getPathFromRequest(request, "GET ");
        getRequest(request);
        std::string type = getType(baseDir);
        std::vector<char> data;
        try {
            data = fileUtils->readAllChars(baseDir);
            handle(data, type);
        } catch (std::exception &e) {
            std::string s = "<html><head><title>My Server</title></head><body><h1>404</h1></body></html>";
            data = strToCharVector(s);
        }
        std::stringstream ss;
        ss << "HTTP/1.1 200\n"
                << "content-length: " << data.size() << "\n"
                << "cache-control: no-cache\n"
                << "content-type: " << type << " text/html; charset=utf-8\n"
                << "connection: close\n\n";
        std::string header = ss.str();
        data.insert(data.begin(), header.begin(), header.end());
        return data;
    }

private:
    void handle(std::vector<char> &content, std::string type) {
        if (type.compare("text/html") == 0) {
            for (const auto [key,name]: fileUtils->getEnvMap()) {
                replaceAll(content, strToCharVector(key), strToCharVector(name));
            }
            // removeComments(string).getBytes();
        } else {
        }
    }

public:
    void debug() {
        // std::vector<char> response = createResponse("GET / HTTP/1.1");
        std::string s =
                "href=${site_base}/feed/ href=${site_base}/feed/  href=${site_base}/feed/  href=${site_base}/feed/  href=${site_base}/feed/  href=${site_base}/feed/  href=${site_base}/feed/  href=${site_base}/feed/ ";
        std::vector<char> response = strToCharVector(s);
        handle(response, "text/html");
        std::string str(response.begin(), response.end());
        std::cout << str << std::endl;
    }

    Page();

    ~Page();
};

Page::Page() : SocketListerner() {
}

Page::~Page() {
}
