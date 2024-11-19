package proxy.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private static AppConfig config;
    private static Map<String, AppConfig.Service> serviceMap;

    ConfigLoader() {}

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
            inputStream = externalInputStream;
        } catch (FileNotFoundException e) {
            logger.warn("config.yaml not found in external location: {}", ymlPath);

            // If file is not found, fallback to loading from the classpath
            final InputStream resourceInputStream = ConfigLoader.class.getClassLoader()
                    .getResourceAsStream("config.yaml");
            if (resourceInputStream == null) {
                throw new RuntimeException("config.yaml not found in the classpath or external path");
            } else {
                logger.info("Loaded config from classpath resource: {}", ymlPath);
                inputStream = resourceInputStream;
            }
        }

        // Load the YAML config and process it
        try (final InputStream finalInputStream = inputStream) {
            Map<String, Object> yamlMap = yaml.load(finalInputStream);

            // Replace placeholders with environment variable values and remove them if not found
            replaceEnvVars(yamlMap);

            // Convert the modified YAML map back to the AppConfig object
            config = yaml.loadAs(yaml.dump(yamlMap), AppConfig.class);

            validateConfig(config);  // Validate the loaded config
            createServiceMap(config.getServices());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Failed to load and parse config.yaml", e.getMessage());
            throw new RuntimeException("Failed to load and parse config.yaml", e);
        }
    }

    // Replace environment variable placeholders and remove them if not found
    private static void replaceEnvVars(Map<String, Object> yamlMap) {
        for (Map.Entry<String, Object> entry : yamlMap.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof String) {
                String stringValue = (String) value;
                // Match environment variables in the form of ${ENV_VAR_NAME:default_value}
                Pattern pattern = Pattern.compile("\\$\\{([A-Za-z0-9_]+):([^}]+)\\}");
                Matcher matcher = pattern.matcher(stringValue);

                // Replace placeholders with environment variable values or use default if not found
                while (matcher.find()) {
                    String envVar = matcher.group(1);
                    String defaultValue = matcher.group(2);
                    String envValue = System.getenv(envVar);

                    if (envValue != null) {
                        stringValue = stringValue.replace(matcher.group(0), envValue);
                        logger.info("Mapped environment variable: {} = {}", envVar, envValue);
                    } else {
                        // If environment variable is not found, replace with default value
                        stringValue = stringValue.replace(matcher.group(0), defaultValue);
                        logger.info("Environment variable {} not found, using default: {}", envVar, defaultValue);
                    }
                }
                entry.setValue(stringValue);  // Update the entry with the replaced value
            } else if (value instanceof Map) {
                replaceEnvVars((Map<String, Object>) value);  // Recursive call for nested maps
            }
        }
    }

    public static void createServiceMap(List<AppConfig.Service> services) {
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
