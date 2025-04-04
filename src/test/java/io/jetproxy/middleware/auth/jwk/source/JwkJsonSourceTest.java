package io.jetproxy.middleware.auth.jwk.source;

import io.jetproxy.context.AppContext;
import io.jetproxy.middleware.cache.Cache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.security.interfaces.RSAPublicKey;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

class JwkJsonSourceTest {

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
    void parseJwks_shouldConvertPEMCertToPublicKey() throws Exception {
        // Clean and working test certificate (from mkcert or similar)
        String pem = """
                -----BEGIN CERTIFICATE-----
                MIIBszCCAVmgAwIBAgIUE5F5U6AUN7C3lL7e5UobgXtMEXowDQYJKoZIhvcNAQEL
                BQAwEjEQMA4GA1UEAwwHSmV0UHJveHkwHhcNMjQwNDA1MDAwMDAwWhcNMjgwNDA1
                MDAwMDAwWjASMRAwDgYDVQQDDAdKZXRQcm94eTCBnzANBgkqhkiG9w0BAQEFAAOB
                jQAwgYkCgYEAtrXvDYpGvSvhTJ+1OavIfBvwfrms+6kb5mfyF3fyRMW++3CmNOIL
                mpKszXw5sWUBRY9WgDx6kDbGH1AmR8zqSnYI4VZtbOybSAw6rG8O4AacibIFtIGs
                p5iA6RmAPo7wKCVBrGVaCJlPNSmthzFUTf3YQZ5eA4N4TZYfD9zgdU8CAwEAAaNT
                MFEwHQYDVR0OBBYEFMJGy1az58flGSoZasIHrc2L5NdZMB8GA1UdIwQYMBaAFMJG
                y1az58flGSoZasIHrc2L5NdZMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQEL
                BQADgYEAKU8glWDFbbE9gDFEEynICDk2ZmfI+AYa0YBYYpp+r2d3vLkHPCuv9nPZ
                5NVP5zH5r+zZ3fGPYxvcjQ2scfY9Uuf1RS+4UWeDDpHo+51ET0q1Dc9+yrq1nE6H
                gvRIdz3YXro4vnD7TULy1K2XFC1eY5TD2AqBod+ueOoZ2Etqks0=
                -----END CERTIFICATE-----
                """;

        String jwksJson = """
        {
          "my-key": "%s"
        }
        """.formatted(
                pem.replace("\n", "\\n") // escape for JSON
        );

        JwkX509Source source = new JwkX509Source("dummy", 60L);
        Map<String, RSAPublicKey> map = source.parseJwks(jwksJson);

        assertEquals(1, map.size());
        RSAPublicKey key = map.get("my-key");

        assertNotNull(key);
        assertEquals(new java.math.BigInteger("65537"), key.getPublicExponent());
        assertTrue(key.getModulus().bitLength() > 1024);
    }
}
