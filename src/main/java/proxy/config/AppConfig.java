package proxy.config;

import java.util.List;

public class AppConfig {

    private int port;
    private List<ProxyRule> proxies;

    public List<ProxyRule> getProxies() {
        return proxies;
    }

    public void setProxies(List<ProxyRule> proxies) {
        this.proxies = proxies;
    }
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
    public static class ProxyRule {
        private String path;
        private String target;
        private String middleware;
        private List<String> methods;

        public List<String> getMethods() {
            return methods;
        }

        public void setMethods(List<String> methods) {
            this.methods = methods;
        }

        public String getMiddleware() {
            return middleware;
        }

        public void setMiddleware(String middleware) {
            this.middleware = middleware;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }
    }
}
