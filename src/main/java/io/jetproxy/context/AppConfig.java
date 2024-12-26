package io.jetproxy.context;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.json.JSONPropertyName;

import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Getter
@Setter
@ToString
public class AppConfig {
    private String appName;
    private int port;
    private int defaultTimeout;
    private boolean dashboard;
    private boolean debugMode = false;
    private String rootPath;
    private Storage storage;
    private List<Proxy> proxies;
    private List<Service> services;
    private List<User> users;
    private CorsFilter corsFilter = new CorsFilter(); // Default values if missing

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
        private List<String> accessControlAllowHeaders = List.of(
                "*");
        private List<String> accessControlAllowOriginList = List.of(
                "*");

        public List<String> getAccessControlAllowMethods() {
            if (accessControlAllowMethods.contains("*")) {
                return List.of( "GET",
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

    public static class Proxy {
        private String path;
        private String service;
        private Middleware middleware = null;
        private long ttl;
        private String uuid;

        public boolean hasMiddleware() {
            return middleware != null;
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
    public static class Middleware {
        private Headers header;
        private String basicAuth; // Updated basicAuth field to be nullable
        private ForwardAuth forwardAuth;
        private String rule = "";
        private CircuitBreaker circuitBreaker;
        private RateLimiter rateLimiter;
        private Bulkhead bulkhead;
        private Retry retry;

        public boolean hasBasicAuth() {
            return basicAuth != null;
        }

        public boolean hasForwardAuth() {
            return forwardAuth != null;
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
    }

    @Getter
    @Setter
    @ToString
    public static class ForwardAuth {
        private String path;
        private String service;
        private String requestHeaders;
        private String responseHeaders;

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
        // Derived method for Retry-After
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
    public  static class Retry {
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
        // Derived method for Retry-After
        public int getRetryAfterSeconds() {
            return (int) Math.ceil(maxWaitDuration / 1000.0); // Convert ms to seconds
        }
    }

    @Getter
    @Setter
    @ToString
    public static class Service {
        private String name;
        private String url;
        private List<String> methods;
        private String role;
        private String healthcheck;
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
        private StatsdConfig statsd;
        private InMemoryConfig inMemory;

        @Getter
        @Setter
        @ToString
        public static class RedisConfig {
            private boolean enabled;
            private String host;
            private int port;
            private int database;
            private int maxTotal = 128;
            private int maxIdle = 64;
            private int minIdle = 16;
        }

        @Getter
        @Setter
        @ToString
        public static class StatsdConfig {
            private boolean enabled;
            private String host;
            private int port;
            private String prefix;
        }

        @Getter
        @Setter
        @ToString
        public static class InMemoryConfig {
            private boolean enabled;
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
}
