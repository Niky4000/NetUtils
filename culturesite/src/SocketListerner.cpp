//
// Created by me on 02/09/2026.
//

#include <iostream>
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>

class SocketListerner {
private:
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
            close(client_fd); // Close client connection
        }

        close(server_fd); // Close listening socket
        return 0;
    }

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
