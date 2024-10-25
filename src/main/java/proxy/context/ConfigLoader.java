package proxy.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import proxy.service.holder.ProxyHolder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private static AppConfig config;

    ConfigLoader() {}
    private static Map<String, AppConfig.Service> serviceMap;
    public static AppConfig getConfig(String ymlPath) {
        if (config == null) {
            try {
                loadConfig(ymlPath);  // Load the config if it's not already loaded
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return config;
    }

    public static void loadConfig(String ymlPath) throws IOException {
        Yaml yaml = new Yaml();
        InputStream inputStream = null;

        // First try to load the file from the external location using FileInputStream
        try {
            final InputStream externalInputStream = new FileInputStream(ymlPath);
            logger.info("Loaded config from external file: {}", ymlPath);
            inputStream = externalInputStream;  // Only set once, after successful external load
        } catch (FileNotFoundException e) {
            logger.warn("config.yaml not found in external location: {}", ymlPath);

            // If file is not found, fallback to loading from the classpath
            final InputStream resourceInputStream = ConfigLoader.class.getClassLoader()
                    .getResourceAsStream("config.yaml");
            if (resourceInputStream == null) {
                throw new RuntimeException("config.yaml not found in the classpath or external path");
            } else {
                logger.info("Loaded config from classpath resource: {}", ymlPath);
                inputStream = resourceInputStream;  // Only set once, after successful classpath load
            }
        }

        // Load the YAML config and process it
        try (final InputStream finalInputStream = inputStream) {  // Mark inputStream as final
            config = yaml.loadAs(finalInputStream, AppConfig.class);
            validateConfig(config);  // Validate the loaded config
            createServiceMap(config.getServices());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Failed to load and parse config.yaml", e.getMessage());
            throw new RuntimeException("Failed to load and parse config.yaml", e);
        }
    }

    private static void createServiceMap(List<AppConfig.Service> services) {
        serviceMap = new HashMap<>();
        for (AppConfig.Service service : services) {
            serviceMap.put(service.getName(), service);
        }
    }

    // Validate the loaded configuration
    public static void validateConfig(AppConfig config) {
        if (config.getPort() <= 0 || config.getPort() > 65535) {
            throw new IllegalArgumentException("Invalid port number: " + config.getPort());
        }

        if (config.getDefaultTimeout() <= 0) {
            throw new IllegalArgumentException("Default timeout must be greater than 0");
        }

        validateProxies(config.getProxies());
        validateServices(config.getServices());
    }

    public static void validateProxies(List<AppConfig.Proxy> proxies) {
        if (proxies == null || proxies.isEmpty()) {
            throw new IllegalArgumentException("No proxies configured");
        }
        for (AppConfig.Proxy proxy : proxies) {
            if (proxy.getPath() == null || proxy.getPath().isEmpty()) {
                throw new IllegalArgumentException("Proxy path cannot be null or empty");
            }
            if (proxy.getService() == null || proxy.getService().isEmpty()) {
                throw new IllegalArgumentException("Proxy service cannot be null or empty for path: " + proxy.getPath());
            }

        }
    }

    public static void validateServices(List<AppConfig.Service> services) {
        if (services == null || services.isEmpty()) {
            throw new IllegalArgumentException("No services configured");
        }
        for (AppConfig.Service service : services) {
            if (service.getName() == null || service.getName().isEmpty()) {
                throw new IllegalArgumentException("Service name cannot be null or empty");
            }
            if (service.getUrl() == null || service.getUrl().isEmpty()) {
                throw new IllegalArgumentException("Service URL cannot be null or empty for service: " + service.getName());
            }
        }
    }

    public static Map<String, AppConfig.Service> getServiceMap() {
        return serviceMap;
    }

    public static void setServiceMap(Map<String, AppConfig.Service> serviceMap) {
        ConfigLoader.serviceMap = serviceMap;
    }
}
