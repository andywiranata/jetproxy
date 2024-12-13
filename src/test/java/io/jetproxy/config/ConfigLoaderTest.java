package io.jetproxy.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import io.jetproxy.context.AppConfig;
import io.jetproxy.context.ConfigLoader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

    private AppConfig appConfig;

    @BeforeEach
    void setUp() {
        appConfig = new AppConfig();
        ConfigLoader.setServiceMap(null);  // Reset the service map before each test
    }

    @Test
    void testLoadConfigFromValidYaml() throws Exception {
        // Simulate YAML content for a valid configuration
        String yamlContent = "port: 8080\n" +
                "defaultTimeout: 5000\n" +
                "proxies:\n" +
                "  - path: /api\n" +
                "    service: test-service\n" +
                "services:\n" +
                "  - name: test-service\n" +
                "    url: http://localhost:8080";
        InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes());

        // Load configuration using a real Yaml instance
        Yaml yaml = new Yaml();
        AppConfig config = yaml.loadAs(inputStream, AppConfig.class);

        assertNotNull(config);
        assertEquals(8080, config.getPort());
        assertEquals(5000, config.getDefaultTimeout());

        // Load the mock config
        ConfigLoader.loadConfig("config.yaml");

        // Assertions for services and proxies
        assertNotNull(config.getServices());
        assertFalse(config.getServices().isEmpty());
        assertNotNull(config.getProxies());
        assertFalse(config.getProxies().isEmpty());
    }

    @Test
    void testServiceMapCreation() {
        // Create test services
        AppConfig.Service service = new AppConfig.Service();
        service.setName("test-service");
        service.setUrl("http://localhost:8080");

        List<AppConfig.Service> services = Arrays.asList(service);
        ConfigLoader.setServiceMap(null);  // Reset serviceMap

        // Call the createServiceMap() method
        ConfigLoader.createServiceMap(services);

        Map<String, AppConfig.Service> serviceMap = ConfigLoader.getServiceMap();
        assertNotNull(serviceMap);
        assertEquals(1, serviceMap.size());
        assertEquals("http://localhost:8080", serviceMap.get("test-service").getUrl());
    }

    @Test
    void testInvalidPortInConfigThrowsException() {
        // Set an invalid port
        appConfig.setPort(-1);

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> ConfigLoader.validateConfig(appConfig),
                "Expected validateConfig() to throw an exception for an invalid port"
        );
        assertTrue(thrown.getMessage().contains("Invalid port number"));
    }

    @Test
    void testNoProxiesConfiguredThrowsException() {
        // Set proxies as null
        appConfig.setProxies(null);

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> ConfigLoader.validateProxies(appConfig.getProxies()),
                "Expected validateProxies() to throw an exception when proxies are null"
        );
        assertTrue(thrown.getMessage().contains("No proxies configured"));
    }

    @Test
    void testNoServicesConfiguredThrowsException() {
        // Set services as null
        appConfig.setServices(null);

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> ConfigLoader.validateServices(appConfig.getServices()),
                "Expected validateServices() to throw an exception when services are null"
        );
        assertTrue(thrown.getMessage().contains("No services configured"));
    }

    @Test
    void testInvalidServiceUrlThrowsException() {
        // Set up a service with an invalid URL
        AppConfig.Service invalidService = new AppConfig.Service();
        invalidService.setName("invalid-service");
        invalidService.setUrl("");  // Invalid URL

        List<AppConfig.Service> services = Arrays.asList(invalidService);
        appConfig.setServices(services);

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> ConfigLoader.validateServices(appConfig.getServices()),
                "Expected validateServices() to throw an exception for an invalid URL"
        );
        assertTrue(thrown.getMessage().contains("Service URL cannot be null or empty"));
    }

    @Test
    void testGetConfigLoadsOnlyOnce() throws Exception {
        // Load configuration
        ConfigLoader.loadConfig("config.yaml");

        // Call getConfig() multiple times to ensure it only loads once
        AppConfig firstConfig = ConfigLoader.getConfig("config.yaml");
        AppConfig secondConfig = ConfigLoader.getConfig("config.yaml");

        assertSame(firstConfig, secondConfig, "getConfig() should load configuration only once");
    }
}
