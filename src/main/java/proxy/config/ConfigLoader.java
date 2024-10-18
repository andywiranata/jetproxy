package proxy.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigLoader {

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

    private static void loadConfig(String ymlPath) throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = ConfigLoader.class.getClassLoader().getResourceAsStream(ymlPath)) {
            if (inputStream == null) {
                throw new RuntimeException("config.yaml not found in the resources folder");
            }
            config = yaml.loadAs(inputStream, AppConfig.class);
            validateConfig(config);  // Validate the loaded config
            createServiceMap(config.getServices());
        }
    }

    private static void createServiceMap(List<AppConfig.Service> services) {
        serviceMap = new HashMap<>();
        for (AppConfig.Service service : services) {
            serviceMap.put(service.getName(), service);
        }
    }

    // Validate the loaded configuration
    private static void validateConfig(AppConfig config) {
        if (config.getPort() <= 0 || config.getPort() > 65535) {
            throw new IllegalArgumentException("Invalid port number: " + config.getPort());
        }

        if (config.getDefaultTimeout() <= 0) {
            throw new IllegalArgumentException("Default timeout must be greater than 0");
        }

        validateProxies(config.getProxies());
        validateServices(config.getServices());
    }

    private static void validateProxies(List<AppConfig.Proxy> proxies) {
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
            if (proxy.getMethods() == null || proxy.getMethods().isEmpty()) {
                throw new IllegalArgumentException("Proxy methods cannot be null or empty for path: " + proxy.getPath());
            }
        }
    }

    private static void validateServices(List<AppConfig.Service> services) {
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
