package proxy.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderVOTTest {
    private ConfigLoaderVO configLoader;

    @BeforeEach
    void setUp() {
        configLoader = new ConfigLoaderVO();
    }

    @Test
    void testLoadConfig_validConfig() throws Exception {
        // Assume we have a valid YAML file
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("valid_config.yaml")) {
            assertNotNull(inputStream, "valid_config.yaml not found");
            AppConfig config = ConfigLoaderVO.getConfig();
            assertNotNull(config);
            assertEquals(8080, config.getPort());
            assertTrue(config.getDefaultTimeout() > 0);
            assertFalse(config.getProxies().isEmpty());
            assertFalse(config.getServices().isEmpty());
        }
    }

    @Test
    void testLoadConfig_invalidPort() {
        // Mock an invalid configuration with an invalid port number
        AppConfig config = new AppConfig();
        config.setPort(70000); // Invalid port
        config.setDefaultTimeout(1000);
        config.setProxies(List.of());
        config.setServices(List.of());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ConfigLoaderVO.getConfig();
        });

        String expectedMessage = "Invalid port number: 70000";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void testCreateServiceMap() {
        // Mock a configuration
        AppConfig config = new AppConfig();
        AppConfig.Service service1 = new AppConfig.Service();
        service1.setName("service1");
        service1.setUrl("http://service1.com");
        AppConfig.Service service2 = new AppConfig.Service();
        service2.setName("service2");
        service2.setUrl("http://service2.com");

        config.setServices(List.of(service1, service2));

        // Load service map
        ConfigLoaderVO.setServiceMap(Map.of(
                "service1", "http://service1.com",
                "service2", "http://service2.com"
        ));

        Map<String, String> serviceMap = ConfigLoaderVO.getServiceMap();
        assertEquals(2, serviceMap.size());
        assertEquals("http://service1.com", serviceMap.get("service1"));
        assertEquals("http://service2.com", serviceMap.get("service2"));
    }

    @Test
    void testValidateConfig_invalidServiceName() {
//        // Create a service with an empty name
//        AppConfig config = new AppConfig();
//        AppConfig.Service invalidService = new AppConfig.Service();
//        invalidService.setName(""); // Invalid name
//        invalidService.setUrl("http://service.com");
//        config.setServices(List.of(invalidService));
//
//        // Validate the configuration
//        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
//            ConfigLoaderVO.validateServices(config.getServices());
//        });
//
//        String expectedMessage = "Service name cannot be null or empty";
//        String actualMessage = exception.getMessage();
//        assertTrue(actualMessage.contains(expectedMessage));
    }

    // Add more tests for other validation scenarios...
}
