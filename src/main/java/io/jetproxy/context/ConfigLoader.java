package io.jetproxy.context;

import io.jetproxy.exception.JetProxyValidationException;
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
    private static Map<String, AppConfig.Service> serviceMap = new HashMap<>();

    @Getter
    private static Map<String, AppConfig.GrpcService> grpcServiceMap = new HashMap<>();

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
            ConfigValidator.validateConfig(config);
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
     * Creates a map of services by their name for quick lookup.
     */
    public static void createGrpcServiceMap(List<AppConfig.GrpcService> grpcServices) {
        grpcServiceMap = new HashMap<>();
        for (AppConfig.GrpcService service : grpcServices) {
            grpcServiceMap.put(service.getName(), service);
        }
    }

    /**
     * Updates the service map dynamically.
     */
    public static void setServiceMap(Map<String, AppConfig.Service> serviceMap) {
        ConfigLoader.serviceMap = serviceMap;
    }

    public static  void addOrUpdateProxies(List<AppConfig.Proxy> updatedProxies) {
        logger.info("Adding or updating proxies: {}", updatedProxies);

        // Validate the incoming proxies
        ConfigValidator.validateProxies(config.getProxies(), config.getServices(), config.getGrpcServices());

        // Get the current list of proxies
        List<AppConfig.Proxy> existingProxies = config.getProxies();
        Map<String, AppConfig.Proxy> proxyMap = existingProxies.stream()
                .collect(Collectors.toMap(AppConfig.Proxy::getPath, proxy -> proxy));

        // Update or add new proxies
        for (AppConfig.Proxy updatedProxy : updatedProxies) {
            if (proxyMap.containsKey(updatedProxy.getPath())) {
                logger.info("Updating existing proxy: {}", updatedProxy.getPath());
                AppConfig.Proxy existingProxy = proxyMap.get(updatedProxy.getPath());

                // Update fields selectively
                if (updatedProxy.getService() != null) {
                    existingProxy.setService(updatedProxy.getService());
                }
                if (updatedProxy.getTtl() > 0) {
                    existingProxy.setTtl(updatedProxy.getTtl());
                }
                if (updatedProxy.getMiddleware() != null) {
                    existingProxy.setMiddleware(updatedProxy.getMiddleware());
                }
            } else {
                logger.info("Adding new proxy: {}", updatedProxy.getPath());
                existingProxies.add(updatedProxy);
            }
        }
        // Ensure the updated proxy list is valid
        ConfigValidator.validateProxies(existingProxies, config.getServices(), config.getGrpcServices());
        // Log the updated proxies for confirmation
        logger.info("Updated proxies: {}", existingProxies);
    }

    public static void addOrUpdateServices(List<AppConfig.Service> updatedServices) {
        logger.info("Adding or updating services: {}", updatedServices);

        // Validate the incoming services
        ConfigValidator.validateServices(updatedServices);

        // Get the current list of services
        List<AppConfig.Service> existingServices = config.getServices();
        Map<String, AppConfig.Service> serviceMap = existingServices.stream()
                .collect(Collectors.toMap(AppConfig.Service::getName, service -> service));

        // Update or add new services
        for (AppConfig.Service updatedService : updatedServices) {
            if (serviceMap.containsKey(updatedService.getName())) {
                logger.info("Updating existing service: {}", updatedService.getName());
                AppConfig.Service existingService = serviceMap.get(updatedService.getName());

                // Update fields selectively
                if (updatedService.getUrl() != null && !updatedService.getUrl().isEmpty()) {
                    existingService.setUrl(updatedService.getUrl());
                }
                if (updatedService.getMethods() != null && !updatedService.getMethods().isEmpty()) {
                    existingService.setMethods(updatedService.getMethods());
                }
                if (updatedService.getRole() != null) {
                    existingService.setRole(updatedService.getRole());
                }
                if (updatedService.getHealthcheck() != null) {
                    existingService.setHealthcheck(updatedService.getHealthcheck());
                }
            } else {
                logger.info("Adding new service: {}", updatedService.getName());
                existingServices.add(updatedService);
            }
        }

        // Ensure the updated service list is valid
        ConfigValidator.validateServices(existingServices);

        // Update the service map
        createServiceMap(existingServices);

        // Log the updated services for confirmation
        logger.info("Updated services: {}", getServiceMap());
    }

}
