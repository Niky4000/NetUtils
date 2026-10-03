//
// Created by me on 02/09/2026.
//
#include <algorithm>
#include <cstring>
#include <iostream>
#include <ranges>
#include <thread>
#ifndef _WIN32
#include <signal.h>
#endif
#ifdef _WIN32
#include <winsock2.h>
#else
#include <sys/socket.h>
#include <sstream>
#include <netinet/in.h>
#include <unistd.h>
#endif
#include <filesystem>
#include "FileUtils.cpp"
#include <unordered_set>
#include <openssl/ssl.h>
#include <openssl/err.h>

#ifdef _WIN32
using Socket = SOCKET;
#else
using Socket = int;
#endif

class SocketListerner {
protected:
    FileUtils *fileUtils = new FileUtils();

    virtual std::vector<char> createResponse(std::string request) {
        std::string data = "<html><head><title>My Server</title></head><body><h1>Hello, World!</h1></body></html>";
        std::stringstream ss;
        ss << "HTTP/1.1 200\n"
                << "content-length: " << data.length() << "\n"
                << "cache-control: no-cache\n"
                << "content-type: text/html\n"
                << "connection: close\n\n";
        ss << data;
        return strToCharVector(ss.str());
    }

    std::vector<char> strToCharVector(std::string str) {
        const char *source = str.c_str();
        size_t length = std::strlen(source);
        std::vector<char> ret(source, source + length);
        return ret;
    }

    std::string replaceAll(std::string str, const std::string &from, const std::string &to) {
        size_t pos = 0;
        while ((pos = str.find(from, pos)) != std::string::npos) {
            str.replace(pos, from.length(), to);
            pos += to.length(); // Advance past the replaced part
        }
        return str;
    }

    void replaceAll(std::vector<char> &vec, const std::vector<char> &from, const std::vector<char> &to) {
        if (from.empty()) return;
        // 1. Find the substring in the vector
        auto it = std::search(vec.begin(), vec.end(), from.begin(), from.end());
        // 2. If found, replace it
        while (it != vec.end()) {
            size_t index = std::distance(vec.begin(), it);
            // Erase the old substring
            it = vec.erase(it, it + from.size());
            // Insert the new substring
            vec.insert(it, to.begin(), to.end());
            it = std::search(vec.begin(), vec.end(), from.begin(), from.end());
        }
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
            if (substring.compare("css") == 0) {
                return "text/css; charset=utf-8";
            } else if (substring.compare("js") == 0) {
                return "text/javascript; charset=utf-8";
            } else if (substring.compare("png") == 0) {
                return "image/png";
            } else if (substring.compare("jpeg") == 0) {
                return "image/jpeg";
            } else if (substring.compare("jpg") == 0) {
                return "image/jpeg";
            } else if (substring.compare("svg") == 0) {
                return "image/svg+xml";
            } else if (substring.compare("ico") == 0) {
                return "image/x-icon";
            } else if (substring.compare("pdf") == 0) {
                return "application/pdf";
            } else {
                return "text/html; charset=utf-8";
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

    std::unordered_set<std::string> requests = {"GET"};

    void getRequest(std::string request) {
        for (auto requestHead: requests) {
            if (request.contains(requestHead)) {
                int startIndex = request.find("GET") + requestHead.length();
                int endIndex = request.find(" ", startIndex + 1);
                int endIndex2 = request.find("&", startIndex + 1);
                int endIndex3 = request.find("\n", startIndex + 1);
                int indexTo = min({endIndex, endIndex2, endIndex3});
                if (indexTo - startIndex > 0) {
                    std::string requestPath = request.substr(startIndex + 1, indexTo - startIndex);
                    // std::cout << requestPath << std::endl;
                }
            }
        }
    }

private:
    // std::string basePath = std::filesystem::current_path().string();
    std::string basePath = formatBasePath(fileUtils->getConfig("base")); // Взять это из настроек!
    std::string sslCertificatePath = formatBasePath(fileUtils->getConfig("ssl_certificate_path")); // Взять это из настроек!
    std::string sslPrivateKeyPath = formatBasePath(fileUtils->getConfig("ssl_private_key_path")); // Взять это из настроек!
    std::string endStr = "\n";
    std::string backVariableName = "back=";

    std::string formatBasePath(std::string basePath) {
        std::string lastSymbol = basePath.substr(basePath.length() - 1, basePath.length());
        return lastSymbol == "/" ? basePath.substr(0, basePath.length() - 1) : basePath;
    }

    bool isParentDir(std::string request) {
        return request.substr(0, request.find(endStr)).contains(backVariableName);
    }

    std::string handle(std::string str) {
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
                if (request.find("\r\n\r\n") != std::string::npos) {
                    break;
                }
            } else if (bytes_received == 0) {
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
        std::vector<char> response = createResponse(request);
        send(client_fd, response.data(), static_cast<int>(response.size()), 0);
        // std::cout << "HTTP request:\n" << request << std::endl;
#ifdef _WIN32
        closesocket(client_fd);
#else
        close(client_fd);
#endif
    }

    // Теперь функция обработки принимает объект SSL* вместо обычного client_fd
    void answerSsl_old(SSL *ssl, int client_fd) {
        // Внутри этой функции вместо read/recv используйте: SSL_read(ssl, buffer, size);
        // Вместо write/send используйте: SSL_write(ssl, response, size);

        // Примерная логика работы внутри answer:
        /*
        char buffer[1024] = {0};
        int bytes = SSL_read(ssl, buffer, sizeof(buffer));
        if (bytes > 0) {
            const char* reply = "HTTP/1.1 200 OK\r\nContent-Length: 12\r\n\r\nHello HTTPS!";
            SSL_write(ssl, reply, strlen(reply));
        }
        */

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
        std::string request;
        char buffer[1024];
        while (true) {
#ifdef _WIN32
            int bytes_received = SSL_read(ssl, buffer, sizeof(buffer));
#else
            ssize_t bytes_received = SSL_read(ssl, buffer, sizeof(buffer));
#endif
            if (bytes_received > 0) {
                request.append(buffer, bytes_received);
                if (request.find("\r\n\r\n") != std::string::npos) {
                    break;
                }
            } else if (bytes_received == 0) {
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
        std::vector<char> response = createResponse(request);
        // send(client_fd, response.data(), static_cast<int>(response.size()), 0);
        SSL_write(ssl, response.data(), response.size());
        // std::cout << "HTTP request:\n" << request << std::endl;
#ifdef _WIN32
        // closesocket(client_fd);
        // После завершения работы обязательно корректно закрываем SSL и сокет:
        SSL_shutdown(ssl);
        SSL_free(ssl);
        close(client_fd);
#else
        // После завершения работы обязательно корректно закрываем SSL и сокет:
        SSL_shutdown(ssl);
        SSL_free(ssl);
        close(client_fd);
#endif
    }

    // 1. Метод обработки: ТЕПЕРЬ ОН ТАКОЙ ЖЕ ЧИСТЫЙ, КАК И ДЛЯ ОБЫЧНЫХ СОКЕТОВ
    void answerSsl(SSL *ssl, int client_fd) {
#ifdef _WIN32
        if (client_fd == INVALID_SOCKET) {
            std::cerr << "Invalid client socket: " << WSAGetLastError() << std::endl;
            return;
        }
#else
        if (client_fd < 0) {
            std::cerr << "Invalid client socket" << std::endl;
            return;
        }
#endif

        std::string request;
        char buffer[1024];
        while (true) {
            int bytes_received = SSL_read(ssl, buffer, sizeof(buffer));

            if (bytes_received > 0) {
                request.append(buffer, bytes_received);
                if (request.find("\r\n\r\n") != std::string::npos) {
                    break;
                }
            } else if (bytes_received == 0) {
                break; // Клиент закрыл соединение
            } else {
                int ssl_err = SSL_get_error(ssl, bytes_received);
                if (ssl_err != SSL_ERROR_WANT_READ && ssl_err != SSL_ERROR_WANT_WRITE) {
#ifdef _WIN32
                    std::cerr << "SSL_read failed: " << WSAGetLastError() << " (SSL error: " << ssl_err << ")" << std::endl;
#else
                    std::cerr << "SSL_read failed: " << strerror(errno) << " (SSL error: " << ssl_err << ")" << std::endl;
#endif
                    break;
                }
            }
        }

        // Создаем HTTP ответ и отправляем его через SSL
        std::vector<char> response = createResponse(request);
        SSL_write(ssl, response.data(), static_cast<int>(response.size()));

        // НИКАКИХ SSL_free, SSL_shutdown и close здесь больше нет!
        // Всё управление памятью передано обратно в поток.
    }

    // Функция для инициализации контекста OpenSSL (вызывается один раз)
    SSL_CTX *create_ssl_context() {
        // Инициализируем библиотеку (в современных версиях OpenSSL это происходит автоматически, но для совместимости оставим)
        SSL_library_init();
        OpenSSL_add_all_algorithms();
        SSL_load_error_strings();

        const SSL_METHOD *method = TLS_server_method();
        SSL_CTX *ctx = SSL_CTX_new(method);
        if (!ctx) {
            std::cerr << "Unable to create SSL context" << std::endl;
            ERR_print_errors_fp(stderr);
            return nullptr;
        }

        // Загрузка сертификата (замените "cert.pem" на ваш файл)
        if (SSL_CTX_use_certificate_file(ctx, sslCertificatePath.data(), SSL_FILETYPE_PEM) <= 0) {
            std::cerr << "Failed to load certificate" << std::endl;
            ERR_print_errors_fp(stderr);
            SSL_CTX_free(ctx);
            return nullptr;
        }

        // Загрузка приватного ключа (замените "key.pem" on ваш файл)
        if (SSL_CTX_use_PrivateKey_file(ctx, sslPrivateKeyPath.data(), SSL_FILETYPE_PEM) <= 0) {
            std::cerr << "Failed to load private key" << std::endl;
            ERR_print_errors_fp(stderr);
            SSL_CTX_free(ctx);
            return nullptr;
        }

        // Проверка соответствия ключа сертификату
        if (!SSL_CTX_check_private_key(ctx)) {
            std::cerr << "Private key does not match the certificate public key" << std::endl;
            SSL_CTX_free(ctx);
            return nullptr;
        }

        return ctx;
    }

#ifdef _WIN32
    int startListen(int port) {
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
        address.sin_port = htons(port);
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
        // std::cout << "Server is successfully listening on port " << port << "..." << std::endl;
        while (true) {
            sockaddr_in client_address{};
            int client_len = sizeof(client_address);
            Socket client_fd = accept(server_fd, reinterpret_cast<sockaddr *>(&client_address), &client_len);
            std::thread sendingThread([&]() { answer(client_fd); });
            sendingThread.detach();
        }
        return 0;
    }


#else
    int startListen(int port) {
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
        address.sin_port = htons(port); // Listen on port 8080

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

        // std::cout << "Server is successfully listening on port " << port << "..." << std::endl;

        // 4. Accept a connection (blocks until a client connects)
        sockaddr_in client_address{};
        socklen_t client_len = sizeof(client_address);

        while (true) {
            int client_fd = accept(server_fd, (struct sockaddr *) &client_address, &client_len);
            std::thread sendingThread([&]() { answer(client_fd); });
            sendingThread.detach();
        }

        close(server_fd); // Close listening socket
        return 0;
    }

    int startListenSsl_old(int port) {
        // 0. Создаем глобальный SSL контекст
        SSL_CTX *ctx = create_ssl_context();
        if (!ctx) {
            return 1;
        }

        // 1. Create the socket (IPv4, TCP)
        int server_fd = socket(AF_INET, SOCK_STREAM, 0);
        if (server_fd < 0) {
            std::cerr << "Socket creation failed" << std::endl;
            SSL_CTX_free(ctx);
            return 1;
        }

        int opt = 1;
        if (setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt)) < 0) {
            std::cerr << "setsockopt failed: " << std::strerror(errno) << std::endl;
            close(server_fd);
            SSL_CTX_free(ctx);
            return 1;
        }

        // 2. Bind the socket to an IP and Port
        sockaddr_in address{};
        address.sin_family = AF_INET;
        address.sin_addr.s_addr = INADDR_ANY;
        address.sin_port = htons(port); // Для HTTPS обычно используют порт 443

        if (bind(server_fd, (struct sockaddr *) &address, sizeof(address)) < 0) {
            std::cerr << "Bind failed. errno = " << errno << ", message = " << std::strerror(errno) << std::endl;
            close(server_fd);
            SSL_CTX_free(ctx);
            return 1;
        }

        // 3. Listen for incoming connections
        if (listen(server_fd, SOMAXCONN) < 0) {
            std::cerr << "Listen failed" << std::endl;
            close(server_fd);
            SSL_CTX_free(ctx);
            return 1;
        }

        // 4. Accept a connection
        sockaddr_in client_address{};
        socklen_t client_len = sizeof(client_address);

        while (true) {
            int client_fd = accept(server_fd, (struct sockaddr *) &client_address, &client_len);
            if (client_fd < 0) {
                std::cerr << "Accept failed" << std::endl;
                continue;
            }

            // // Передаем client_fd и ctx по значению, чтобы избежать race condition
            // std::thread sendingThread([client_fd, ctx, this]() {
            //     // Создаем SSL-структуру для конкретного соединения
            //     SSL *ssl = SSL_new(ctx);
            //     SSL_set_fd(ssl, client_fd);
            //
            //     // Выполняем SSL-рукопожатие (Handshake)
            //     if (SSL_accept(ssl) <= 0) {
            //         // Если клиент сбросил соединение или сертификат не подошел
            //         std::cerr << "SSL handshake failed" << std::endl;
            //         ERR_print_errors_fp(stderr);
            //         SSL_free(ssl);
            //         close(client_fd);
            //     } else {
            //         // Если рукопожатие успешно, передаем управление в логику ответа
            //         answerSsl(ssl, client_fd);
            //     }
            // });
            //
            // sendingThread.detach();

            // Передаем client_fd и ctx по значению
            std::thread sendingThread([client_fd, ctx, this]() {
                // Создаем SSL-структуру для конкретного соединения
                SSL *ssl = SSL_new(ctx);
                if (!ssl) {
                    std::cerr << "Failed to create SSL structure" << std::endl;
                    close(client_fd);
                    return;
                }

                SSL_set_fd(ssl, client_fd);

                // Выполняем SSL-рукопожатие (Handshake)
                int handshake_res = SSL_accept(ssl);
                if (handshake_res <= 0) {
                    int ssl_err = SSL_get_error(ssl, handshake_res);

                    // Не все ошибки критичны (например, клиент просто отключился)
                    std::cerr << "SSL handshake failed. OpenSSL Error code: " << ssl_err << std::endl;
                    ERR_print_errors_fp(stderr);

                    // Обязательно освобождаем ресурсы здесь
                    SSL_free(ssl);
                    close(client_fd);
                } else {
                    // Защищаем поток от падения, если в вашей логике ответа возникнет C++ exception
                    try {
                        answerSsl(ssl, client_fd);
                    } catch (const std::exception &e) {
                        std::cerr << "Exception in answerSsl: " << e.what() << std::endl;
                    } catch (...) {
                        std::cerr << "Unknown exception in answerSsl" << std::endl;
                    }

                    // Рекомендуется закрывать SSL и сокет сразу после завершения обработки,
                    // если answerSsl не делает это принудительно сама.
                    // SSL_shutdown(ssl);
                    // SSL_free(ssl);
                    // close(client_fd);
                }
            });
            sendingThread.detach();
        }

        close(server_fd);
        SSL_CTX_free(ctx);
        return 0;
    }


    // 2. Основной метод прослушивания
    int startListenSsl(int port) {
#ifndef _WIN32
        // Игнорируем сигнал SIGPIPE, чтобы операционная система не убивала приложение
        // при внезапном отключении клиентов (браузеров)
        struct sigaction sa;
        sa.sa_handler = SIG_IGN;
        sigemptyset(&sa.sa_mask);
        sa.sa_flags = 0;
        if (sigaction(SIGPIPE, &sa, NULL) < 0) {
            std::cerr << "Failed to ignore SIGPIPE" << std::endl;
            return 1;
        }
#endif
        SSL_CTX *ctx = create_ssl_context();
        if (!ctx) {
            return 1;
        }

        int server_fd = socket(AF_INET, SOCK_STREAM, 0);
        if (server_fd < 0) {
            std::cerr << "Socket creation failed" << std::endl;
            SSL_CTX_free(ctx);
            return 1;
        }

        int opt = 1;
        if (setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, (const char *) &opt, sizeof(opt)) < 0) {
            std::cerr << "setsockopt failed" << std::endl;
#ifdef _WIN32
            closesocket(server_fd);
#else
            close(server_fd);
#endif
            SSL_CTX_free(ctx);
            return 1;
        }

        sockaddr_in address{};
        address.sin_family = AF_INET;
        address.sin_addr.s_addr = INADDR_ANY;
        address.sin_port = htons(port);

        if (bind(server_fd, (struct sockaddr *) &address, sizeof(address)) < 0) {
            std::cerr << "Bind failed. errno = " << errno << std::endl;
#ifdef _WIN32
            closesocket(server_fd);
#else
            close(server_fd);
#endif
            SSL_CTX_free(ctx);
            return 1;
        }

        if (listen(server_fd, SOMAXCONN) < 0) {
            std::cerr << "Listen failed" << std::endl;
#ifdef _WIN32
            closesocket(server_fd);
#else
            close(server_fd);
#endif
            SSL_CTX_free(ctx);
            return 1;
        }

        sockaddr_in client_address{};
        socklen_t client_len = sizeof(client_address);

        while (true) {
            int client_fd = accept(server_fd, (struct sockaddr *) &client_address, &client_len);
#ifdef _WIN32
            if (client_fd == INVALID_SOCKET) {
                std::cerr << "Accept failed" << std::endl;
                continue;
            }
#else
            if (client_fd < 0) {
                std::cerr << "Accept failed" << std::endl;
                continue;
            }
#endif

            // Поток полностью контролирует жизненный цикл соединения
            std::thread sendingThread([client_fd, ctx, this]() {
                SSL *ssl = SSL_new(ctx);
                if (!ssl) {
                    std::cerr << "Failed to create SSL structure" << std::endl;
#ifdef _WIN32
                    closesocket(client_fd);
#endif
                    return;
                }

                SSL_set_fd(ssl, client_fd);

                // ШАГ 1: Заставляем OpenSSL использовать флаг MSG_NOSIGNAL при вызовах send/recv в Linux
                BIO *bio = SSL_get_wbio(ssl);
                if (bio) {
                    BIO_set_nbio_accept(bio, 1); // Позволяет OpenSSL корректно обрабатывать обрывы без сигналов
                }

                int handshake_res = SSL_accept(ssl);
                if (handshake_res <= 0) {
                    int ssl_err = SSL_get_error(ssl, handshake_res);
                    std::cerr << "SSL handshake failed. OpenSSL Error code: " << ssl_err << std::endl;
                    // Не паникуем: браузеры часто обрывают "лишние" параллельные сессии
                } else {
                    try {
                        this->answerSsl(ssl, client_fd);
                    } catch (const std::exception &e) {
                        std::cerr << "Exception in answerSsl: " << e.what() << std::endl;
                    } catch (...) {
                        std::cerr << "Unknown exception in answerSsl" << std::endl;
                    }
                }

                // ГАРАНТИРОВАННАЯ И ЕДИНСТВЕННАЯ ОЧИСТКА ДЛЯ ЛЮБОГО СЦЕНАРИЯ
                SSL_shutdown(ssl);
                SSL_free(ssl);
#ifdef _WIN32
                closesocket(client_fd);
#endif
            });

            sendingThread.detach();
        }

        // Сюда код дойдет только при выходе из while(true)
#ifdef _WIN32
        closesocket(server_fd);
#endif
        SSL_CTX_free(ctx);
        return 0;
    }

#endif

public:
    SocketListerner();

    ~SocketListerner();

    void listenForConnections(int port) {
        startListen(port);
    }

    void listenForHttpsConnections(int port) {
        startListenSsl(port);
    }
};

SocketListerner::SocketListerner() {
    std::cout << "basePath: " << basePath << std::endl;
}

SocketListerner::~SocketListerner() {
    delete fileUtils;
}
