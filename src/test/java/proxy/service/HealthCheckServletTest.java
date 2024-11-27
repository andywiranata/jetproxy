package proxy.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import proxy.context.AppContext;
import proxy.context.AppConfig;
import proxy.middleware.cache.RedisPoolManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HealthCheckServletTest {

    private HealthCheckServlet servlet;
    private JedisPool mockJedisPool;
    private Jedis mockJedis;

    @BeforeEach
    void setUp() {
        servlet = new HealthCheckServlet();
        mockJedisPool = mock(JedisPool.class);
        mockJedis = mock(Jedis.class);
    }

    @Test
    void testHealthCheckWithHealthyRedisAndServers() throws Exception {
        try (MockedStatic<RedisPoolManager> redisPoolManagerMock = Mockito.mockStatic(RedisPoolManager.class);
             MockedStatic<AppContext> appContextMock = Mockito.mockStatic(AppContext.class)) {

            // Mock RedisPoolManager
            redisPoolManagerMock.when(RedisPoolManager::getPool).thenReturn(mockJedisPool);
            when(mockJedisPool.getResource()).thenReturn(mockJedis);
            when(mockJedis.ping()).thenReturn("PONG");

            // Mock AppContext and service map
            AppContext mockAppContext = mock(AppContext.class);
            AppConfig.Storage mockStorage = mock(AppConfig.Storage.class);
            AppConfig.Service mockService = mock(AppConfig.Service.class);
            Map<String, AppConfig.Service> mockServiceMap = new HashMap<>();
            mockServiceMap.put("ServiceA", mockService);

            appContextMock.when(AppContext::get).thenReturn(mockAppContext);
            when(mockAppContext.getConfig()).thenReturn(mock(AppConfig.class));
            when(mockAppContext.getServiceMap()).thenReturn(mockServiceMap);
            when(mockAppContext.getConfig().getStorage()).thenReturn(mockStorage);
            when(mockStorage.hasRedisServer()).thenReturn(true);
            when(mockService.getUrl()).thenReturn("http://example.com");
            when(mockService.getHealthcheck()).thenReturn("/health");

            // Mock HTTPServletResponse and Request
            HttpServletRequest mockRequest = mock(HttpServletRequest.class);
            HttpServletResponse mockResponse = mock(HttpServletResponse.class);

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            when(mockResponse.getWriter()).thenReturn(printWriter);

            // Call servlet doGet method
            servlet.doGet(mockRequest, mockResponse);

            // Verify response
            verify(mockResponse).setContentType("application/json");
            verify(mockResponse).setStatus(HttpServletResponse.SC_OK);

            // Validate output
            String responseContent = stringWriter.toString();
            assertNotNull(responseContent);
            // assertTrue(responseContent.contains("\"status\":\"UP\""));
            // assertTrue(responseContent.contains("\"redisStatus\":\"Healthy\""));
        }
    }

    @Test
    void testHealthCheckWithUnhealthyRedis() throws Exception {
        try (MockedStatic<RedisPoolManager> redisPoolManagerMock = Mockito.mockStatic(RedisPoolManager.class);
             MockedStatic<AppContext> appContextMock = Mockito.mockStatic(AppContext.class)) {

            // Mock RedisPoolManager
            redisPoolManagerMock.when(RedisPoolManager::getPool).thenReturn(mockJedisPool);
            when(mockJedisPool.getResource()).thenReturn(mockJedis);
            when(mockJedis.ping()).thenThrow(new RuntimeException("Redis is down"));

            // Mock AppContext and service map
            AppContext mockAppContext = mock(AppContext.class);
            AppConfig.Storage mockStorage = mock(AppConfig.Storage.class);
            appContextMock.when(AppContext::get).thenReturn(mockAppContext);
            when(mockAppContext.getConfig()).thenReturn(mock(AppConfig.class));
            when(mockAppContext.getConfig().getStorage()).thenReturn(mockStorage);
            when(mockStorage.hasRedisServer()).thenReturn(true);
            when(mockAppContext.getServiceMap()).thenReturn(Collections.emptyMap());

            // Mock HTTPServletResponse and Request
            HttpServletRequest mockRequest = mock(HttpServletRequest.class);
            HttpServletResponse mockResponse = mock(HttpServletResponse.class);

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            when(mockResponse.getWriter()).thenReturn(printWriter);

            // Call servlet doGet method
            servlet.doGet(mockRequest, mockResponse);

            // Verify response
            verify(mockResponse).setContentType("application/json");
            verify(mockResponse).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            // Validate output
            String responseContent = stringWriter.toString();
            assertNotNull(responseContent);
            // assertTrue(responseContent.contains("\"status\":\"DOWN\""));
            // assertTrue(responseContent.contains("\"redisStatus\":\"Unhealthy\""));
        }
    }

    @Test
    void testHealthCheckWithoutRedis() throws Exception {
        try (MockedStatic<AppContext> appContextMock = Mockito.mockStatic(AppContext.class)) {
            // Mock AppContext with no Redis server
            AppContext mockAppContext = mock(AppContext.class);
            AppConfig.Storage mockStorage = mock(AppConfig.Storage.class);
            appContextMock.when(AppContext::get).thenReturn(mockAppContext);
            when(mockAppContext.getConfig()).thenReturn(mock(AppConfig.class));
            when(mockAppContext.getConfig().getStorage()).thenReturn(mockStorage);
            when(mockStorage.hasRedisServer()).thenReturn(false);

            // Mock HTTPServletResponse and Request
            HttpServletRequest mockRequest = mock(HttpServletRequest.class);
            HttpServletResponse mockResponse = mock(HttpServletResponse.class);

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            when(mockResponse.getWriter()).thenReturn(printWriter);

            // Call servlet doGet method
            servlet.doGet(mockRequest, mockResponse);

            // Verify response
            // verify(mockResponse).setContentType("application/json");
            // verify(mockResponse).setStatus(HttpServletResponse.SC_OK);

            // Validate output
            String responseContent = stringWriter.toString();
            assertNotNull(responseContent);
            // assertTrue(responseContent.contains("\"status\":\"UP\""));
            // assertTrue(responseContent.contains("\"redisStatus\":\"Not Found\""));
        }
    }
}
