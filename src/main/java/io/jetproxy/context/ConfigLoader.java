package io.jetproxy.context;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private static AppConfig config;
    /**
     * -- GETTER --
     *  Retrieves the current map of services.
     */
    @Getter
    private static Map<String, AppConfig.Service> serviceMap;

    private ConfigLoader() {}

    /**
     * Loads and returns the application configuration.
     *
     * @param ymlPath The path to the YAML configuration file.
     * @return The loaded AppConfig object.
     */
    public static AppConfig getConfig(String ymlPath) {
        if (config == null) {
            try {
                loadConfig(ymlPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return config;
    }

    /**
     * Loads the configuration from a YAML file.
     *
     * @param ymlPath The path to the YAML configuration file.
     * @throws IOException if the file cannot be read or parsed.
     */
    public static void loadConfig(String ymlPath) throws IOException {
        Yaml yaml = new Yaml();
        InputStream inputStream = null;

        // Try to load the external file
        try {
            inputStream = new FileInputStream(ymlPath);
            logger.info("Loaded config from external file: {}", ymlPath);
        } catch (FileNotFoundException e) {
            logger.warn("Config file not found at {}. Falling back to classpath resource.", ymlPath);
            inputStream = ConfigLoader.class.getClassLoader().getResourceAsStream("config.yaml");
            if (inputStream == null) {
                throw new RuntimeException("Config file not found in external location or classpath.");
            }
        }

        // Load the YAML content
        try (InputStream finalInputStream = inputStream) {
            Map<String, Object> yamlMap = yaml.load(finalInputStream);
            replaceEnvVars(yamlMap);
            config = yaml.loadAs(yaml.dump(yamlMap), AppConfig.class);

            // Validate and create service map
            validateConfig(config);
            createServiceMap(config.getServices());
        } catch (Exception e) {
            logger.error("Failed to load and parse config.yaml: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load and parse config.yaml", e);
        }
    }

    /**
     * Replaces placeholders in the configuration with environment variables or defaults.
     */
    private static void replaceEnvVars(Map<String, Object> yamlMap) {
        for (Map.Entry<String, Object> entry : yamlMap.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                String stringValue = (String) value;
                Pattern pattern = Pattern.compile("\\$\\{([A-Za-z0-9_]+):([^}]+)\\}");
                Matcher matcher = pattern.matcher(stringValue);

                while (matcher.find()) {
                    String envVar = matcher.group(1);
                    String defaultValue = matcher.group(2);
                    String envValue = System.getenv(envVar);
                    stringValue = stringValue.replace(matcher.group(0), envValue != null ? envValue : defaultValue);
                }
                entry.setValue(stringValue);
            } else if (value instanceof Map) {
                replaceEnvVars((Map<String, Object>) value);
            }
        }
    }

    /**
     * Creates a map of services by their name for quick lookup.
     */
    public static void createServiceMap(List<AppConfig.Service> services) {
        serviceMap = new HashMap<>();
        for (AppConfig.Service service : services) {
            serviceMap.put(service.getName(), service);
        }
    }

    /**
     * Validates the entire configuration.
     */
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

    /**
     * Validates the list of proxies and ensures they reference valid services.
     */
    public static void validateProxies(List<AppConfig.Proxy> proxies) {
        if (proxies == null || proxies.isEmpty()) {
            throw new IllegalArgumentException("No proxies configured");
        }

        List<AppConfig.Service> services = config.getServices();
        if (services == null || services.isEmpty()) {
            throw new IllegalArgumentException("No services configured, but proxies depend on them.");
        }

        Set<String> registeredServiceNames = services.stream()
                .map(AppConfig.Service::getName)
                .collect(Collectors.toSet());

        for (AppConfig.Proxy proxy : proxies) {
            if (proxy.getPath() == null || proxy.getPath().isEmpty()) {
                throw new IllegalArgumentException("Proxy path cannot be null or empty");
            }
            if (proxy.getService() == null || proxy.getService().isEmpty()) {
                throw new IllegalArgumentException("Proxy service cannot be null or empty for path: " + proxy.getPath());
            }
            if (!registeredServiceNames.contains(proxy.getService())) {
                throw new IllegalArgumentException("Proxy references an unregistered service: " + proxy.getService()
                        + " for path: " + proxy.getPath());
            }
            if (!proxy.getPath().startsWith("/")) {
                throw new IllegalArgumentException("Proxy path must start with '/': " + proxy.getPath());
            }
        }
    }

    /**
     * Validates the list of services.
     */
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
            if (!service.getUrl().startsWith("http://") && !service.getUrl().startsWith("https://")) {
                throw new IllegalArgumentException("Service URL must start with 'http://' or 'https://': " + service.getUrl());
            }
        }
    }

    /**
     * Updates the service map dynamically.
     */
    public static void setServiceMap(Map<String, AppConfig.Service> serviceMap) {
        ConfigLoader.serviceMap = serviceMap;
    }
    /**
     * Updates a proxy in the configuration.
     *
     * @param updatedProxy The proxy to update or add.
     */
    public static synchronized void updateProxy(AppConfig.Proxy updatedProxy) {
        logger.info("Updating proxy: {}", updatedProxy.getPath());
        List<AppConfig.Proxy> proxies = config.getProxies();
        proxies.removeIf(proxy -> proxy.getPath().equals(updatedProxy.getPath())); // Remove existing
        proxies.add(updatedProxy); // Add or update
        validateProxies(proxies); // Validate updated proxies
        logger.info("Proxy updated successfully: {}", updatedProxy.getPath());
    }

    /**
     * Removes a proxy from the configuration.
     *
     * @param path The path of the proxy to remove.
     */
    public static synchronized void removeProxy(String path) {
        logger.info("Removing proxy: {}", path);
        List<AppConfig.Proxy> proxies = config.getProxies();
        boolean removed = proxies.removeIf(proxy -> proxy.getPath().equals(path));
        if (!removed) {
            logger.warn("Proxy not found for path: {}", path);
            return;
        }
        validateProxies(proxies); // Validate remaining proxies
        logger.info("Proxy removed successfully: {}", path);
    }

    /**
     * Updates a service in the configuration.
     *
     * @param updatedService The service to update or add.
     */
    public static synchronized void updateService(AppConfig.Service updatedService) {
        logger.info("Updating service: {}", updatedService.getName());
        List<AppConfig.Service> services = config.getServices();
        services.removeIf(service -> service.getName().equals(updatedService.getName())); // Remove existing
        services.add(updatedService); // Add or update
        validateServices(services); // Validate updated services
        createServiceMap(services); // Update service map
        logger.info("Service updated successfully: {}", updatedService.getName());
    }

    /**
     * Removes a service from the configuration.
     *
     * @param serviceName The name of the service to remove.
     */
    public static synchronized void removeService(String serviceName) {
        logger.info("Removing service: {}", serviceName);
        List<AppConfig.Service> services = config.getServices();
        boolean removed = services.removeIf(service -> service.getName().equals(serviceName));
        if (!removed) {
            logger.warn("Service not found: {}", serviceName);
            return;
        }
        validateServices(services); // Validate remaining services
        createServiceMap(services); // Update service map
        logger.info("Service removed successfully: {}", serviceName);
    }
}
