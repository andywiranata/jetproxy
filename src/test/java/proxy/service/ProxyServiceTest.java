package proxy.service;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import proxy.config.AppConfig;
import proxy.config.ConfigLoaderVO;
import proxy.service.ProxyService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProxyServiceTest {
    private ProxyService proxyService;

    private AppConfig config;
    private ServletContextHandler context;

    @BeforeEach
    void setUp() {
        config = mock(AppConfig.class);
        context = mock(ServletContextHandler.class);
        proxyService = new ProxyService(config);
    }

    @Test
    void setupProxies_shouldAddProxiesToContext() {
        // Arrange
        AppConfig.Proxy proxy = new AppConfig.Proxy();
        proxy.setPath("/test");
        proxy.setService("testService");

        // Mocking the behavior of config
        when(config.getProxies()).thenReturn(List.of(proxy));
        // Mock the service map retrieval
        when(ConfigLoaderVO.getServiceMap()).thenReturn(Map.of("testService", "http://testservice.com"));

        // Act
        proxyService.setupProxies(context);

        //
        // Assert
        verify(context).addServlet(any(ServletHolder.class), eq("/test/*"));
    }

    @Test
    void setupProxies_shouldThrowException_whenServiceUrlNotFound() {
        // Arrange
        AppConfig.Proxy proxy = new AppConfig.Proxy();
        proxy.setPath("/test");
        proxy.setService("nonExistentService");

        when(config.getProxies()).thenReturn(List.of(proxy));
        when(ConfigLoaderVO.getServiceMap()).thenReturn(Collections.emptyMap());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            proxyService.setupProxies(context);
        });
    }
}
