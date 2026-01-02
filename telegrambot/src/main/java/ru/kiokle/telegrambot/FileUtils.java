package ru.kiokle.telegrambot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import ru.kiokle.telegrambot.bean.OrderBean;
import ru.kiokle.telegrambot.bean.OrderConfigsBean;

public class FileUtils {

    private static final String DATABASE_FILE = "db";
    private static final String CONFIGS_FILE = "configs.properties";
    private static final String SYSTEM_CONFIGS_FILE = "system.properties";
    OrderConfigsBean configs;
    private static String s = FileSystems.getDefault().getSeparator();
    private File configFile;
    private File systemConfigFile;
    private File databaseFile;
    Properties systemProperties;

    public FileUtils() {
        init();
    }

    private void init() {
        configFile = new File(getPathToJar().getParent() + s + CONFIGS_FILE);
        systemConfigFile = new File(getPathToJar().getParent() + s + SYSTEM_CONFIGS_FILE);
        databaseFile = new File(getPathToJar().getParent() + s + DATABASE_FILE);
        if (!configFile.exists()) {
            configFile = new File(getPathToJar().getParentFile().getParent() + s + CONFIGS_FILE);
            systemConfigFile = new File(getPathToJar().getParentFile().getParent() + s + SYSTEM_CONFIGS_FILE);
            databaseFile = new File(getPathToJar().getParentFile().getParent() + s + DATABASE_FILE);
        }
    }

    public OrderConfigsBean getConfigs() throws IOException {
        if (configs == null) {
            systemProperties = new Properties();
            systemProperties.load(new FileInputStream(systemConfigFile));
            List<OrderBean> orders = new ObjectMapper().readValue(configFile, new TypeReference<List<OrderBean>>() {
            });
            Map<String, String> map = orders.stream().collect(Collectors.toMap(OrderBean::getType, OrderBean::getName));
            configs = new OrderConfigsBean(orders, map);
        }
        return configs;
    }

    public File getPathToJar() {
        try {
            File file = new File(FileUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (file.getAbsolutePath().contains("classes")) { // Launched from debugger!
                file = Arrays.stream(file.getParentFile().listFiles()).filter(localFile -> localFile.getName().endsWith(".jar")).findFirst().get();
            }
            return file;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public File getDatabaseFile() {
        return databaseFile;
    }

    public Properties getSystemProperties() {
        return systemProperties;
    }
}
