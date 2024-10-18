package proxy.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigLoaderTest {

    private Yaml yamlMock;
    private AppConfig appConfigMock;

    @BeforeEach
    void setUp() {
        // Mock Yaml and AppConfig
        yamlMock = Mockito.mock(Yaml.class);
        appConfigMock = Mockito.mock(AppConfig.class);
    }

    @Test
    void testLoadValidConfig() throws Exception {
        // Prepare a mock input stream with valid YAML content
        String yamlContent = "port: 8080\n" +
                "defaultTimeout: 5000\n" +
                "proxies:\n" +
                "  - path: /api\n" +
                "    service: test-service\n" +
                "    methods: [GET, POST]\n" +
                "services:\n" +
                "  - name: test-service\n" +
                "    url: http://localhost:8080";

        InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes());

        // Mock the Yaml.loadAs() method to return a valid AppConfig instance
        AppConfig.Service service = new AppConfig.Service();
        service.setName("test-service");
        service.setUrl("http://localhost:8080");
        List<AppConfig.Service> services = Arrays.asList(service);

        AppConfig.Proxy proxy = new AppConfig.Proxy();
        proxy.setPath("/api");
        proxy.setService("test-service");
        proxy.setMethods(Arrays.asList("GET", "POST"));
        List<AppConfig.Proxy> proxies = Arrays.asList(proxy);

        when(appConfigMock.getPort()).thenReturn(8080);
        when(appConfigMock.getDefaultTimeout()).thenReturn(5000);
        when(appConfigMock.getServices()).thenReturn(services);
        when(appConfigMock.getProxies()).thenReturn(proxies);

        // Use Mockito to mock Yaml loading behavior
        when(yamlMock.loadAs(inputStream, AppConfig.class)).thenReturn(appConfigMock);

        // Call the method under test
        ConfigLoader.loadConfig("config.yaml");

        // Verify that the configuration was loaded correctly
        AppConfig config = ConfigLoader.getConfig("config.yaml");
        assertNotNull(config);
        assertEquals(8080, config.getPort());
        assertEquals(5000, config.getDefaultTimeout());
        assertFalse(config.getProxies().isEmpty());
        assertFalse(config.getServices().isEmpty());

        // Validate the service map was created
        assertNotNull(ConfigLoader.getServiceMap());
        assertEquals("tasksApi", ConfigLoader.getServiceMap().get("tasksApi").getName());
    }

    @Test
    void testInvalidPortInConfig() {
        // Mock invalid port
        when(appConfigMock.getPort()).thenReturn(-1);
        when(appConfigMock.getDefaultTimeout()).thenReturn(5000);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> ConfigLoader.validateConfig(appConfigMock),
                "Expected to throw, but it didn't"
        );
        assertTrue(thrown.getMessage().contains("Invalid port number"));
    }

    @Test
    void testNoServicesConfigured() {
        // Mock no services
        when(appConfigMock.getServices()).thenReturn(null);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> ConfigLoader.validateServices(null),
                "Expected to throw, but it didn't"
        );
        assertTrue(thrown.getMessage().contains("No services configured"));
    }

    @Test
    void testNoProxiesConfigured() {
        // Mock no proxies
        when(appConfigMock.getProxies()).thenReturn(null);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> ConfigLoader.validateProxies(null),
                "Expected to throw, but it didn't"
        );
        assertTrue(thrown.getMessage().contains("No proxies configured"));
    }
}
