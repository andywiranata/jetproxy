package proxy.context;

import io.github.resilience4j.core.lang.Nullable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Optional;

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
        private String auth = "";                // for auth configurations, e.g., 'basicAuth:roleA'
        private String rule = "";                // for rules configuration
        private CircuitBreaker circuitBreaker; // optional CircuitBreaker configuration
        public boolean hasCircuitBreaker() {
            return circuitBreaker != null;
        }
    }

    @Getter
    @Setter
    @ToString
    public static class CircuitBreaker {
        private boolean enabled = false;            // Default: disabled
        private int failureThreshold = 50;          // Default: 50% failure rate threshold
        private int slowCallThreshold = 50;         // Default: 50% slow call rate threshold
        private int slowCallDuration = 2000;        // Default: 2 seconds for slow calls
        private int openStateDuration = 10;         // Default: 10 seconds in open state
        private int waitDurationInOpenState = 1000; // Default: 10-second wait in open state
        private int permittedNumberOfCallsInHalfOpenState = 10; // Default: 3 calls in half-open state
        private int minimumNumberOfCalls = 5;        // Default: Minimum of 5 calls before evaluating

    }

    @Getter
    @Setter
    @ToString
    public static class Service {
        private String name;
        private String url;
        private List<String> methods;
        private String role; // role field for access control
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
            private long maxMemory = 50; // MB
            private int size = 10000; // updated to match YAML
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
}
