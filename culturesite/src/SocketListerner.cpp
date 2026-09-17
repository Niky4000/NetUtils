//
// Created by me on 02/09/2026.
//
#include <algorithm>
#include <cstring>
#include <iostream>
#include <ranges>
#ifdef _WIN32
#include <winsock2.h>
#else
#include <sys/socket.h>
#include <sstream>
#include <netinet/in.h>
#include <unistd.h>
#endif
#include <filesystem>

#ifdef _WIN32
using Socket = SOCKET;
#else
using Socket = int;
#endif

class SocketListerner {
protected:
    virtual std::string createResponse(std::string request) {
        std::string data = "<html><head><title>My Server</title></head><body><h1>Hello, World!</h1></body></html>";
        std::stringstream ss;
        ss << "HTTP/1.1 200\n"
                << "content-length: " << data.length() << "\n"
                << "cache-control: no-cache\n"
                << "content-type: text/html\n"
                << "connection: close\n\n";
        ss << data;
        return ss.str();
    }

    int getClosestIndex(std::string str) {
        int i = 1;
        int index2 = str.find("?");
        int startingIndex = 0;
        while (i > 0 && i < index2) {
            i = str.find(".", startingIndex + 1);
            if (i > 0 && i < index2) {
                startingIndex = i;
            }
        };
        return startingIndex;
    }

    std::string getType(std::filesystem::path file) {
        try {
            std::string name = file.filename().string() + "\n";
            int index = name.contains("?") ? getClosestIndex(name) : name.find(".");
            int index2 = name.find("?", index);
            int index3 = name.find("\n");
            std::string substring = name.substr(index + 1, min({index2, index3}) - (index + 1));
            if (substring.compare("css")) {
                return "text/css";
            } else if (substring.compare("js")) {
                return "text/javascript";
            } else if (substring.compare("png")) {
                return "image/png";
            } else if (substring.compare("jpeg")) {
                return "image/jpeg";
            } else if (substring.compare("jpg")) {
                return "image/jpeg";
            } else if (substring.compare("svg")) {
                return "image/svg+xml";
            } else if (substring.compare("ico")) {
                return "image/x-icon";
            } else {
                return "text/html";
            }
        } catch (std::exception e) {
            // e.printStackTrace();
            throw std::runtime_error("getType exception!");
        }
    }

    std::filesystem::path getPathFromRequest(std::string request, std::string fieldName) {
        if (request.contains(fieldName)) {
            int startIndex = request.find(fieldName) + fieldName.length();
            int endIndex = request.find(" ", startIndex);
            int endIndex2 = request.find("&", startIndex);
            int endIndex3 = request.find("\n", startIndex);
            int indexTo = min({endIndex, endIndex2, endIndex3});
            std::string toHandle = request.substr(startIndex, indexTo - startIndex);
            std::string replaced = replaceAll(replaceAll(replaceAll(toHandle, "%2F", "/"), "%3A", ":"), "%5C", "\\");
            std::string dirStr = handle(replaced);
            std::filesystem::path dir = std::filesystem::path{dirStr};
            if (std::filesystem::exists(dir)) {
                if (isParentDir(request)) {
                    std::filesystem::path parentDir = dir.parent_path();
                    if (std::filesystem::exists(parentDir)) {
                        return parentDir;
                    } else {
                        return dir;
                    }
                } else {
                    return dir;
                }
            } else {
                // std::string baseDir = FileUtils.getPathToJar().getParent();
                // return new File(baseDir);
                return std::filesystem::path{basePath};
            }
        } else {
            // String baseDir = FileUtils.getPathToJar().getParent();
            // return new File(baseDir);
            return std::filesystem::path{basePath};
        }
    }

private:
    std::string basePath = std::filesystem::current_path().string();
    // std::string basePath = "/home/me/Булки/duslyk/bread63"; // Взять это из настроек!
    std::string endStr = "\n";
    std::string backVariableName = "back=";

    bool isParentDir(std::string request) {
        return request.substr(0, request.find(endStr)).contains(backVariableName);
    }

    std::string handle(std::string str) {
        std::cout << ("path = " + str);
        std::filesystem::path d = std::filesystem::path{basePath + str};
        return std::filesystem::is_directory(d) ? basePath + str + "index.html" : basePath + str;
    }

    int min(std::initializer_list<int> i) {
        // 1. Фильтруем элементы >= 0
        auto filtered = i | std::views::filter([](int j) { return j >= 0; });
        // 2. Ищем минимальный элемент
        auto it = std::min_element(filtered.begin(), filtered.end());
        // Если подходящих элементов нет (аналог пустого Optional в getAsInt)
        if (it == filtered.end()) {
            throw std::runtime_error("No value present");
        }
        return *it;
    }

    std::string replaceAll(std::string str, const std::string &from, const std::string &to) {
        size_t pos = 0;
        while ((pos = str.find(from, pos)) != std::string::npos) {
            str.replace(pos, from.length(), to);
            pos += to.length(); // Advance past the replaced part
        }
        return str;
    }

    //     void answer_old(Socket client_fd) {
    // #ifdef _WIN32
    //         if (client_fd == INVALID_SOCKET) {
    //             std::cerr << "Accept failed: " << WSAGetLastError() << std::endl;
    //             return;
    //         }
    // #else
    //         if (client_fd < 0) {
    //             std::cerr << "Accept failed" << std::endl;
    //             return;
    //         }
    // #endif
    //         std::cout << "Client connected successfully!" << std::endl;
    //         char buffer[1024];
    // #ifdef _WIN32
    //         int bytes_received = recv(client_fd, buffer, sizeof(buffer) - 1, 0);
    // #else
    //         ssize_t bytes_received = recv(client_fd, buffer, sizeof(buffer) - 1, 0);
    // #endif
    //         if (bytes_received > 0) {
    //             buffer[bytes_received] = '\0';
    //             std::string response = createResponse();
    //             send(client_fd, response.c_str(), static_cast<int>(strlen(response.c_str())), 0);
    //             std::cout << "Received: " << buffer << std::endl;
    //         } else if (bytes_received == 0) {
    //             std::cout << "Client disconnected" << std::endl;
    //         } else {
    // #ifdef _WIN32
    //             std::cerr << "recv() failed: " << WSAGetLastError() << std::endl;
    // #else
    //             std::cerr << "recv() failed" << std::endl;
    // #endif
    //         }
    // #ifdef _WIN32
    //         closesocket(client_fd);
    // #else
    //         close(client_fd);
    // #endif
    //     }

    void answer(Socket client_fd) {
#ifdef _WIN32
        if (client_fd == INVALID_SOCKET) {
            std::cerr << "Accept failed: " << WSAGetLastError() << std::endl;
            return;
        }
#else
        if (client_fd < 0) {
            std::cerr << "Accept failed" << std::endl;
            return;
        }
#endif
        std::cout << "Client connected successfully!" << std::endl;
        std::string request;
        char buffer[1024];
        while (true) {
#ifdef _WIN32
            int bytes_received = recv(client_fd, buffer, sizeof(buffer), 0);
#else
            ssize_t bytes_received = recv(client_fd, buffer, sizeof(buffer), 0);
#endif
            if (bytes_received > 0) {
                request.append(buffer, bytes_received);
                // HTTP headers are finished
                if (request.find("\r\n\r\n") != std::string::npos) {
                    break;
                }
            } else if (bytes_received == 0) {
                std::cout << "Client disconnected" << std::endl;
                break;
            } else {
#ifdef _WIN32
                std::cerr << "recv() failed: " << WSAGetLastError() << std::endl;
#else
                std::cerr << "recv() failed: " << strerror(errno) << std::endl;
#endif
                break;
            }
        }
        // Create HTTP response
        std::string response = createResponse(request);
        send(client_fd, response.c_str(), static_cast<int>(response.size()), 0);
        std::cout << "HTTP request:\n" << request << std::endl;
#ifdef _WIN32
        closesocket(client_fd);
#else
        close(client_fd);
#endif
    }

#ifdef _WIN32
    int startListen() {
        WSADATA wsaData;
        if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0) {
            std::cerr << "WSAStartup failed" << std::endl;
            return 1;
        }
        Socket server_fd = socket(AF_INET,SOCK_STREAM, IPPROTO_TCP);
        if (server_fd == INVALID_SOCKET) {
            std::cerr << "socket() failed: " << WSAGetLastError() << std::endl;
            WSACleanup();
            return 1;
        }
        sockaddr_in address{};
        address.sin_family = AF_INET;
        address.sin_addr.s_addr = INADDR_ANY;
        address.sin_port = htons(8080);
        if (bind(server_fd, reinterpret_cast<sockaddr *>(&address), sizeof(address)) == SOCKET_ERROR) {
            std::cerr << "bind() failed: " << WSAGetLastError() << std::endl;
            closesocket(server_fd);
            WSACleanup();
            return 1;
        }
        if (listen(server_fd, SOMAXCONN) == SOCKET_ERROR) {
            std::cerr << "listen() failed: " << WSAGetLastError() << std::endl;
            closesocket(server_fd);
            WSACleanup();
            return 1;
        }
        std::cout << "Server is listening on port 8080..." << std::endl;
        while (true) {
            sockaddr_in client_address{};
            int client_len = sizeof(client_address);
            Socket client_fd = accept(server_fd, reinterpret_cast<sockaddr *>(&client_address), &client_len);
            answer(client_fd);
        }
        return 0;
    }


#else
    int startListen() {
        // 1. Create the socket (IPv4, TCP)
        int server_fd = socket(AF_INET, SOCK_STREAM, 0);
        if (server_fd < 0) {
            std::cerr << "Socket creation failed" << std::endl;
            return 1;
        }

        // Позволяем повторно использовать локальный адрес/порт
        // после закрытия предыдущего TCP-соединения.
        int opt = 1;
        if (setsockopt(server_fd, SOL_SOCKET,SO_REUSEADDR, &opt, sizeof(opt)) < 0) {
            std::cerr << "setsockopt failed: " << std::strerror(errno) << std::endl;
            close(server_fd);
            return 1;
        }

        // 2. Bind the socket to an IP and Port
        sockaddr_in address{};
        address.sin_family = AF_INET;
        address.sin_addr.s_addr = INADDR_ANY; // Listen on all available interfaces
        address.sin_port = htons(8080); // Listen on port 8080

        if (bind(server_fd, (struct sockaddr *) &address, sizeof(address)) < 0) {
            std::cerr << "Bind failed. errno = " << errno << ", message = " << std::strerror(errno) << std::endl;
            close(server_fd);
            return 1;
        }

        // 3. Listen for incoming connections
        // SOMAXCONN requests the maximum reasonable backlog queue size
        if (listen(server_fd, SOMAXCONN) < 0) {
            std::cerr << "Listen failed" << std::endl;
            close(server_fd);
            return 1;
        }

        std::cout << "Server is successfully listening on port 8080..." << std::endl;

        // 4. Accept a connection (blocks until a client connects)
        sockaddr_in client_address{};
        socklen_t client_len = sizeof(client_address);

        while (true) {
            int client_fd = accept(server_fd, (struct sockaddr *) &client_address, &client_len);
            answer(client_fd);
        }

        close(server_fd); // Close listening socket
        return 0;
    }

    // void answer(int client_fd) {
    //     if (client_fd < 0) {
    //         std::cerr << "Accept failed" << std::endl;
    //     } else {
    //         std::cout << "Client connected successfully!" << std::endl;
    //         // Buffer for incoming data
    //         char buffer[1024];
    //         ssize_t bytes_received = recv(client_fd, buffer, sizeof(buffer) - 1, 0);
    //         if (bytes_received > 0) {
    //             buffer[bytes_received] = '\0';
    //             std::cout << "Received: " << buffer << std::endl;
    //             // Send response
    //             const char *response = "Hello from server!";
    //             send(client_fd, response, strlen(response), 0);
    //         } else if (bytes_received == 0) {
    //             std::cout << "Client disconnected" << std::endl;
    //         } else {
    //             std::cerr << "recv() failed" << std::endl;
    //         }
    //         close(client_fd); // Close client connection
    //     }
    // }

#endif

public:
    SocketListerner();

    ~SocketListerner();

    void listenForConnections() {
        startListen();
        std::cout << "Listening for connections..." << std::endl;
    }
};

SocketListerner::SocketListerner() {
}

SocketListerner::~SocketListerner() {
}
