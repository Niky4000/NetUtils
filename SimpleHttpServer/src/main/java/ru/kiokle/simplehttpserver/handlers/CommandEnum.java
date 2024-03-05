package ru.kiokle.simplehttpserver.handlers;

public enum CommandEnum {
    LENGTH("LENGTH"), UPLOAD("UPLOAD"), EXEC("EXEC"), REMOVE("REMOVE"), MD5("MD5");

    private String command;

    private CommandEnum(String command) {
        this.command = command;
    }
}
