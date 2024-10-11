package proxy.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

public class ConfigLoaderVO {

    // Hold the single instance of ProxyConfig
    private static AppConfig config;

    // Private constructor to prevent instantiation
    private ConfigLoaderVO() {}

    // Public method to access the singleton instance of the config
    public static AppConfig getConfig() throws Exception {
        if (config == null) {
            loadConfig();  // Load the config if it's not already loaded
        }
        return config;
    }

    // Private method to load the configuration from YAML file
    private static void loadConfig() throws Exception {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = ConfigLoaderVO.class.getClassLoader().getResourceAsStream("config.yaml")) {
            if (inputStream == null) {
                throw new RuntimeException("config.yaml not found in the resources folder");
            }
            config = yaml.loadAs(inputStream, AppConfig.class);
        }
    }
}
