//
// Created by me on 02/09/2026.
//
#include <cstring>
#include <iostream>
#ifdef _WIN32
#include <winsock2.h>
//#pragma comment(lib, "ws2_32.lib") // Link with Winsock library
#else
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>
#endif

class SocketListerner {
private:
#ifdef _WIN32
    int startListen() {
        // Initialize Winsock
        WSADATA wsaData;
        if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0) {
            std::cerr << "Winsock initialization failed" << std::endl;
            return 1;
        }

        // Create Socket
        SOCKET server_fd = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);

        // Bind ... (Identical layout to POSIX code above)
        sockaddr_in address{};
        address.sin_family = AF_INET;
        address.sin_addr.s_addr = INADDR_ANY;
        address.sin_port = htons(8080);
        bind(server_fd, (struct sockaddr *) &address, sizeof(address));

        // Listen
        if (listen(server_fd, SOMAXCONN) == SOCKET_ERROR) {
            std::cerr << "Listen failed with error: " << WSAGetLastError() << std::endl;
            closesocket(server_fd);
            WSACleanup();
            return 1;
        }

        std::cout << "Windows Server listening on port 8080..." << std::endl;

        // Clean up
        closesocket(server_fd);
        WSACleanup();
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
        int client_fd = accept(server_fd, (struct sockaddr *) &client_address, &client_len);

        if (client_fd < 0) {
            std::cerr << "Accept failed" << std::endl;
        } else {
            std::cout << "Client connected successfully!" << std::endl;


            // 4. Accept a connection
            // sockaddr_in client_address{};
            // socklen_t client_len = sizeof(client_address);
            //
            // int client_fd = accept(
            //     server_fd,
            //     (struct sockaddr*)&client_address,
            //     &client_len
            // );
            //
            // if (client_fd < 0) {
            //     std::cerr << "Accept failed" << std::endl;
            // } else {
            //     std::cout << "Client connected successfully!" << std::endl;

                // Buffer for incoming data
                char buffer[1024];
                ssize_t bytes_received = recv(client_fd,buffer,sizeof(buffer) - 1, 0);
                if (bytes_received > 0) {
                    buffer[bytes_received] = '\0';
                    std::cout << "Received: " << buffer << std::endl;
                    // Send response
                    const char* response = "Hello from server!";
                    send(client_fd,response, strlen(response),0);
                } else if (bytes_received == 0) {
                    std::cout << "Client disconnected" << std::endl;
                } else {
                    std::cerr << "recv() failed" << std::endl;
                }
            // }

            close(client_fd); // Close client connection
        }

        close(server_fd); // Close listening socket
        return 0;
    }
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
