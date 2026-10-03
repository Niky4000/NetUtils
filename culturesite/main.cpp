#include <iostream>
#include <memory>

#include "src/Page.cpp"
// TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

int getIndexOf(std::vector<std::string> vec, std::string name) {
    auto it = std::find(vec.begin(), vec.end(), name);
    if (it != vec.end()) {
        size_t index = std::distance(vec.begin(), it);
        return index;
    } else {
        return -1;
    }
}

std::string getConfig(std::string arg, std::vector<std::string> argList) {
    int indexOf = getIndexOf(argList, arg);
    if (indexOf >= 0) {
        return argList.at(indexOf + 1);
    } else {
        return "";
    }
}

int main(int argc, const char *argv[]) {
    // TIP Press <shortcut actionId="RenameElement"/> when your caret is at the <b>lang</b> variable name to see how CLion can help you rename it.
    std::setlocale(LC_ALL, "ru");
    std::vector<std::string> argList(argv, argv + argc);
    std::unordered_set<std::string> argSet(argList.begin(), argList.end());
    if (argSet.contains("-port")) {
        int port = std::stoi(getConfig("-port", argList));
        auto socketListerner = std::make_unique<Page>();
        if (argSet.contains("-ssl") || argSet.contains("-https")) {
            socketListerner->listenForHttpsConnections(port);
        } else {
            socketListerner->listenForConnections(port);
        }
        // socketListerner->debug();
    }else {
        std::cout << "Set -port and -ssl command line arguments!" << std::endl;
    }
    return 0;
    // TIP See CLion help at <a href="https://www.jetbrains.com/help/clion/">jetbrains.com/help/clion/</a>. Also, you can try interactive lessons for CLion by selecting 'Help | Learn IDE Features' from the main menu.
}
