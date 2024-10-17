package proxy.config;

import java.util.List;

public class AppConfig {
    private int port;
    private int defaultTimeout;
    private List<Proxy> proxies;
    private List<Service> services;
    private int maxCacheMemory; // use MB

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

    public int getMaxCacheMemory() {
        return maxCacheMemory;
    }

    public void setMaxCacheMemory(int maxCacheMemory) {
        this.maxCacheMemory = maxCacheMemory;
    }

    public static class Proxy {
        private String path;
        private String service;
        private String middleware;
        private long ttl = -1;
        private List<String> methods;

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

        public List<String> getMethods() {
            return methods;
        }

        public void setMethods(List<String> methods) {
            this.methods = methods;
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

    }
}
