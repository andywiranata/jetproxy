package io.jetproxy.middleware.auth.jwk.source;

import io.jetproxy.context.AppContext;
import io.jetproxy.middleware.cache.Cache;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.security.interfaces.RSAPublicKey;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BaseJwkSourceTest {

    private Cache mockCache;
    private MockedStatic<AppContext> appContextMock;

    @BeforeEach
    void setup() {
        mockCache = mock(Cache.class);
        appContextMock = mockStatic(AppContext.class);

        AppContext mockAppContext = mock(AppContext.class);
        when(mockAppContext.getCache()).thenReturn(mockCache);
        when(mockAppContext.getInstanceId()).thenReturn("test-instance");

        appContextMock.when(AppContext::get).thenReturn(mockAppContext);
    }

    @AfterEach
    void tearDown() {
        appContextMock.close();
    }

    @Test
    void shouldReturnKeyFromCacheIfExists() throws Exception {
        // Arrange
        String kid = "test-kid";
        String fakeJwksJson = "{fake-jwks-json}";
        RSAPublicKey expectedKey = mock(RSAPublicKey.class);

        when(mockCache.get("HTTP:JWT:AUTH:SRC:test-instance:" + kid)).thenReturn(fakeJwksJson);

        BaseJwkSource source = new DummyJwkSource(
                "http://localhost:8080/jwks", 300, fakeJwksJson, Map.of(kid, expectedKey)
        );

        // Act
        RSAPublicKey result = source.getPublicKey(kid);

        // Assert
        assertEquals(expectedKey, result);
    }

    @Test
    void shouldFetchAndCacheIfNotInCache() throws Exception {
        // Arrange
        String kid = "key123";
        String fakeJwksJson = "{fake-jwks}";
        RSAPublicKey expectedKey = mock(RSAPublicKey.class);

        when(mockCache.get(any())).thenReturn(null);

        BaseJwkSource source = new DummyJwkSource(
                "http://localhost:8080/jwks", 120, fakeJwksJson, Map.of(kid, expectedKey)
        );

        // Act
        RSAPublicKey result = source.getPublicKey(kid);

        // Assert
        assertNotNull(result);
        assertEquals(expectedKey, result);

        verify(mockCache).put(
                eq("http_jwt_auth_source::test-instance::" + kid),
                eq(fakeJwksJson),
                eq(120L)
        );
    }

    // Custom subclass that overrides fetchJwks + parseJwks
    static class DummyJwkSource extends BaseJwkSource {
        private final String mockResponse;
        private final Map<String, RSAPublicKey> keys;

        DummyJwkSource(String jwksUri, long ttl, String mockResponse, Map<String, RSAPublicKey> keys) {
            super(jwksUri, ttl);
            this.mockResponse = mockResponse;
            this.keys = keys;
        }

        @Override
        protected String fetchJwks() {
            return mockResponse;
        }

        @Override
        protected Map<String, RSAPublicKey> parseJwks(String jwksResponse) {
            return keys;
        }
    }
}
