package proxy.context;

import java.util.List;

public class AppConfig {
    private int port;
    private int defaultTimeout;
    private int maxCacheMemory; // in MB
    private boolean dashboard;
    private MetricsConfig metrics;
    private List<Proxy> proxies;
    private List<Service> services;
    private String realmPath;
    private String realmName;

    // Getters and setters
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getDefaultTimeout() {
        return defaultTimeout;
    }

    public void setDefaultTimeout(int defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    public int getMaxCacheMemory() {
        return maxCacheMemory;
    }

    public void setMaxCacheMemory(int maxCacheMemory) {
        this.maxCacheMemory = maxCacheMemory;
    }

    public boolean isDashboard() {
        return dashboard;
    }

    public void setDashboard(boolean dashboard) {
        this.dashboard = dashboard;
    }

    public MetricsConfig getMetrics() {
        return metrics;
    }

    public void setMetrics(MetricsConfig metrics) {
        this.metrics = metrics;
    }

    public List<Proxy> getProxies() {
        return proxies;
    }

    public void setProxies(List<Proxy> proxies) {
        this.proxies = proxies;
    }

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }

    public static class Proxy {
        private String path;
        private String service;
        private String middleware;
        private long ttl;


        // Getters and setters
        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        public String getMiddleware() {
            return middleware;
        }

        public void setMiddleware(String middleware) {
            this.middleware = middleware;
        }

        public long getTtl() {
            return ttl;
        }

        public void setTtl(long ttl) {
            this.ttl = ttl;
        }
    }

    public static class Service {
        private String name;
        private String url;
        private List<String> methods;
        private String role;

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public List<String> getMethods() {
            return methods;
        }

        public void setMethods(List<String> methods) {
            this.methods = methods;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    public static class MetricsConfig {
        private RedisConfig redis;
        private StatsdConfig statsd;
        private InMemoryConfig inMemory;

        // Getters and setters
        public RedisConfig getRedis() {
            return redis;
        }

        public void setRedis(RedisConfig redis) {
            this.redis = redis;
        }

        public StatsdConfig getStatsd() {
            return statsd;
        }

        public void setStatsd(StatsdConfig statsd) {
            this.statsd = statsd;
        }

        public InMemoryConfig getInMemory() {
            return inMemory;
        }

        public void setInMemory(InMemoryConfig inMemory) {
            this.inMemory = inMemory;
        }

        public static class RedisConfig {
            private boolean enabled;
            private String host;
            private int port;
            private int database;

            // Getters and setters
            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getHost() {
                return host;
            }

            public void setHost(String host) {
                this.host = host;
            }

            public int getPort() {
                return port;
            }

            public void setPort(int port) {
                this.port = port;
            }

            public int getDatabase() {
                return database;
            }

            public void setDatabase(int database) {
                this.database = database;
            }
        }

        public static class StatsdConfig {
            private boolean enabled;
            private String host;
            private int port;
            private String prefix;

            // Getters and setters
            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getHost() {
                return host;
            }

            public void setHost(String host) {
                this.host = host;
            }

            public int getPort() {
                return port;
            }

            public void setPort(int port) {
                this.port = port;
            }

            public String getPrefix() {
                return prefix;
            }

            public void setPrefix(String prefix) {
                this.prefix = prefix;
            }
        }

        public static class InMemoryConfig {
            private boolean enabled;

            // Getters and setters
            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }
    }

    public String getRealmPath() {
        return realmPath;
    }

    public void setRealmPath(String realmPath) {
        this.realmPath = realmPath;
    }

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }
}
