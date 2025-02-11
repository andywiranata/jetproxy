package io.jetproxy.context;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.Duration;
import java.util.*;

@Getter
@Setter
@ToString
public class AppConfig {
    private String uuid = UUID.randomUUID().toString();
    private String appName;
    private int port = 80;
    private int defaultTimeout = 10000;
    private boolean dashboard;
    private boolean accessLog = false;
    private String rootPath;
    private Storage storage;
    private List<Proxy> proxies;
    private List<Service> services;
    private List<GrpcService> grpcServices;
    private List<User> users;
    private CorsFilter corsFilter = new CorsFilter(); // Default values if missing
    private JwtAuthSource jwtAuthSource;
    private Logging logging; // Added logging configuration

    public boolean hasCorsFilter() {
        return corsFilter != null;
    }
    public boolean hasEnableRedisStorage() {
        return storage != null && storage.redis != null && storage.redis.enabled;
    }
    public boolean hasEnableInMemoryStorage() {
        return storage != null && storage.inMemory != null && storage.inMemory.enabled;
    }
    @Getter
    @Setter
    @ToString
    public static class CorsFilter {
        private List<String> accessControlAllowMethods = List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "HEAD",
                "OPTIONS",
                "PATCH",
                "TRACE",
                "CONNECT");
        private List<String> accessControlAllowHeaders = List.of("*");
        private List<String> accessControlAllowOriginList = List.of("*");

        public List<String> getAccessControlAllowMethods() {
            if (accessControlAllowMethods == null ||
                    accessControlAllowMethods.contains("*")) {
                return List.of("GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "HEAD",
                        "OPTIONS",
                        "PATCH",
                        "TRACE",
                        "CONNECT");
            }
            return accessControlAllowMethods;
        }
    }

    @Getter
    @Setter
    @ToString
    public static class Logging {
        private Root root;
        private List<Appender> appenders;
        private List<Logger> loggers;
    }
    @Getter
    @Setter
    @ToString
    public static class Root {
        private String level = "INFO"; // Default level
    }

    @Getter
    @Setter
    @ToString
    public static class Appender {
        private String name;
        private String className; // Use className to avoid keyword conflict
        private Encoder encoder;

        @Getter
        @Setter
        @ToString
        public static class Encoder {
            private String pattern = "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"; // Default pattern
        }
    }

    @Getter
    @Setter
    @ToString
    public static class Logger {
        private String name;
        private String level = "INFO"; // Default level
    }

    @Getter
    @Setter
    @ToString
    public static class Proxy {
        private String path;
        private String service;
        private Middleware middleware = null;
        private long ttl;
        private String uuid;
        private List<Match> matches = new ArrayList<>(); // Added rules list

        public boolean hasMiddleware() {
            return middleware != null;
        }

        public boolean hasMatchRules() {
            return matches != null && !matches.isEmpty();
        }

        public String getUuid() {
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString((service + path).getBytes());
        }
    }

    @Getter
    @Setter
    @ToString
    public static class Match {
        private String rule;  // The condition to match, e.g., "Header('Content-Type', 'application/json')"
        private String service; // Target service for the matched rule
    }

    @Getter
    @Setter
    @ToString
    public static class Middleware {
        private Headers header;
        private JwtAuth jwtAuth;
        private String basicAuth; // Updated basicAuth field to be nullable
        private ForwardAuth forwardAuth;
        private String rule = "";
        private CircuitBreaker circuitBreaker;
        private RateLimiter rateLimiter;
        private Bulkhead bulkhead;
        private Retry retry;
        private Mirroring mirroring;

        public boolean hasBasicAuth() {
            return basicAuth != null;
        }

        public boolean hasForwardAuth() {
            return forwardAuth != null && forwardAuth.enabled;
        }

        public boolean hasJwtAuth() {
            return jwtAuth != null && jwtAuth.enabled;
        }

        public boolean hasCircuitBreaker() {
            return circuitBreaker != null && circuitBreaker.enabled;
        }

        public boolean hasRateLimiter() {
            return rateLimiter != null && rateLimiter.enabled;
        }

        public boolean hasBulkHead() {
            return bulkhead != null && rateLimiter.enabled;
        }

        public boolean hasRetry() {
            return retry != null;
        }

        public boolean hasHeaders() {
            return header != null;
        }

        public boolean hasMirroring() {
            return mirroring != null && mirroring.enabled;
        }
    }

    @Getter
    @Setter
    @ToString
    public static class Mirroring {
        private boolean enabled;
        private String mirrorService;
        private int mirrorPercentage;
    }

    @Getter
    @Setter
    @ToString
    public static class ForwardAuth {
        private boolean enabled;
        private String path;
        private String service;
        private String requestHeaders;
        private String responseHeaders;
    }

    @Getter
    @Setter
    @ToString
    public static class JwtAuth {
        private boolean enabled;
    }

    @Getter
    @Setter
    @ToString
    public static class CircuitBreaker {
        private boolean enabled = false;
        private int failureThreshold = 50;
        private int slowCallThreshold = 50;
        private int slowCallDuration = 2000;
        private int openStateDuration = 10;
        private int waitDurationInOpenState = 1000;
        private int permittedNumberOfCallsInHalfOpenState = 10;
        private int minimumNumberOfCalls = 5;

        public int getRetryAfterSeconds() {
            return (int) Math.ceil(waitDurationInOpenState / 1000.0); // Convert ms to seconds
        }
    }

    @Getter
    @Setter
    public static class RateLimiter {
        private boolean enabled = false;              // Feature disabled by default
        private long limitRefreshPeriod = 1000;       // Default 1 second
        private int limitForPeriod = 10;              // Default 10 requests per period
        private Duration timeoutDuration = Duration.ZERO; // No waiting by default
        private int maxBurstCapacity = 20;           // Default burst capacity of 20
    }

    @Getter
    @Setter
    public static class Retry {
        private boolean enabled = false;
        private int maxAttempts = 5;
        private int waitDuration = 1000;
    }

    @Getter
    @Setter
    public static class Bulkhead {
        private boolean enabled = false;
        private int maxConcurrentCalls = 10; // Default: 10 concurrent calls
        private long maxWaitDuration = 500; // Default: 500 ms
    }

    @Getter
    @Setter
    @ToString
    public static class GrpcService {
        private String name;
        private String host;
        private Integer port = 80;
        private String healthcheck;
        public List<String> getMethods() {
            return List.of("GET",
                    "POST",
                    "PUT",
                    "DELETE",
                    "HEAD",
                    "OPTIONS",
                    "PATCH",
                    "TRACE",
                    "CONNECT");
        }
        // dummy http request, to bypass URI validation
        public String getUrl() {
            return "http://" + host + ":" + port;
        }
    }
    @Getter
    @Setter
    @ToString
    public static class Service {
        private String name;
        private String url;
        private List<String> methods = List.of("*");
        private String role;
        private String healthcheck;
        public List<String> getMethods() {
            if (methods.contains("*")) {
                return List.of("GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "HEAD",
                        "OPTIONS",
                        "PATCH",
                        "TRACE",
                        "CONNECT");
            }
            return methods;
        }
        public String getUuid() {
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString((name + url).getBytes());
        }
    }

    @Getter
    @Setter
    @ToString
    public static class Storage {
        private RedisConfig redis;
        private InMemoryConfig inMemory;

        @Getter
        @Setter
        @ToString
        public static class RedisConfig {
            private boolean enabled = false;
            private String host = "localhost";
            private int port = 6379;
            private int database = 1;
            private int maxTotal = 128;
            private int maxIdle = 64;
            private int minIdle = 16;
        }

        @Getter
        @Setter
        @ToString
        public static class InMemoryConfig {
            private boolean enabled = false;
            private long maxMemory = 50;
            private int size = 10000;
        }

        public boolean hasRedisServer() {
            return redis != null && redis.isEnabled();
        }
    }

    @Getter
    @Setter
    @ToString
    public static class User {
        private String username;
        private String password;
        private String role;

        public String getUuid() {
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(("User" + username + password).getBytes());
        }
    }

    @Getter
    @Setter
    @ToString
    public static class Headers {
        private String requestHeaders;
        private String responseHeaders;

        public boolean hasRequestHeaders() {
            return requestHeaders != null;
        }

        public boolean hasResponseHeaders() {
            return responseHeaders != null;
        }
    }

    @Getter
    @Setter
    public static class JwtAuthSource {
        private String headerName;                // HTTP header containing the JWT
        private String tokenPrefix;               // Prefix (e.g., "Bearer ") in the Authorization header
        private String secretKey;                 // Secret key for HS256 (symmetric key)
        private String jwksUri;                   // JWKS URI for RS256 (asymmetric keys)
        private String jwksType = "x509";         //  # Specify type: x509 or jwk
        private long jwksTtl = -1;                 //  Cache Response
        private Map<String, Object> claimValidations = new HashMap<>(); // Optional claims to validate (e.g., iss, aud)
        private Map<String, String> forwardClaims = new HashMap<>();   // Claims to forward as headers (e.g., sub -> X-User-Id)
    }
}
