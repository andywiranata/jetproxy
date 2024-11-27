package proxy.context;

import io.github.resilience4j.core.lang.Nullable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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

    @Getter
    @Setter
    @ToString
    public static class Proxy {
        private String path;
        private String service;
        private Middleware middleware = null;
        private long ttl;
        public boolean hasMiddleware() {
            return middleware != null;
        }
    }

    @Getter
    @Setter
    @ToString
    public static class Middleware {
        @Nullable
        private Headers header;

        @Nullable
        private String basicAuth; // Updated basicAuth field to be nullable

        @Nullable
        private ForwardAuth forwardAuth;

        private String rule = "";

        private CircuitBreaker circuitBreaker;

        public boolean hasBasicAuth() {
            return basicAuth != null;
        }

        public boolean hasForwardAuth() {
            return forwardAuth != null;
        }

        public boolean hasCircuitBreaker() {
            return circuitBreaker != null;
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
        private String authRequestHeaders;
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
    }

    @Getter
    @Setter
    @ToString
    public static class Headers {
        @Nullable
        private String requestHeaders;
        @Nullable
        private String responseHeaders;

        public boolean hasRequestHeaders() {
            return requestHeaders != null;
        }

        public boolean hasResponseHeaders() {
            return responseHeaders != null;
        }
    }
}
