//
// Created by me on 02/09/2026.
//
#include <cstring>
#include <iostream>
#ifdef _WIN32
#include <winsock2.h>
#else
#include <sys/socket.h>
#include <sstream>
#include <netinet/in.h>
#include <unistd.h>
#endif

#ifdef _WIN32
using Socket = SOCKET;
#else
using Socket = int;
#endif

class SocketListerner {
private:

    std::string createResponse() {
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
        char buffer[1024];
#ifdef _WIN32
        int bytes_received = recv(client_fd, buffer, sizeof(buffer) - 1, 0);
#else
        ssize_t bytes_received = recv(client_fd, buffer, sizeof(buffer) - 1, 0);
#endif
        if (bytes_received > 0) {
            buffer[bytes_received] = '\0';
            std::cout << "Received: " << buffer << std::endl;
            std::string response = createResponse();
            send(client_fd, response.c_str(), static_cast<int>(strlen(response.c_str())), 0);
        } else if (bytes_received == 0) {
            std::cout << "Client disconnected" << std::endl;
        } else {
#ifdef _WIN32
            std::cerr << "recv() failed: " << WSAGetLastError() << std::endl;
#else
            std::cerr << "recv() failed" << std::endl;
#endif
        }
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

        // 2. Bind the socket to an IP and Port
        sockaddr_in address{};
        address.sin_family = AF_INET;
        address.sin_addr.s_addr = INADDR_ANY; // Listen on all available interfaces
        address.sin_port = htons(8080); // Listen on port 8080

        if (bind(server_fd, (struct sockaddr *) &address, sizeof(address)) < 0) {
            std::cerr << "Bind failed" << std::endl;
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
