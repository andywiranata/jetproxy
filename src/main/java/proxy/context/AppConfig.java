package proxy.context;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class AppConfig {
    private int port;
    private int defaultTimeout;
    private int maxCacheMemory; // in MB
    private boolean dashboard;
    private Storage storage;
    private List<Proxy> proxies;
    private List<Service> services;
    private String realmPath;
    private String realmName;
    private List<User> users;
    private String appName;
    private String rootPath;

    @Getter
    @Setter
    @ToString
    public static class Proxy {
        private String path;
        private String service;
        private String middleware;
        private long ttl;
        private String rule = "";
    }

    @Getter
    @Setter
    @ToString
    public static class Service {
        private String name;
        private String url;
        private List<String> methods;
        private String role;
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
            private int size = 1000;
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
