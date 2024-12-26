package io.jetproxy.service.appConfig.service;

import io.jetproxy.context.AppConfig;
import io.jetproxy.context.AppContext;
import io.jetproxy.exception.JetProxyValidationException;
import io.jetproxy.service.holder.ProxyConfigurationManager;

import java.util.List;

public class AppConfigService {
    private final ProxyConfigurationManager proxyConfigurationManager;

    public AppConfigService(ProxyConfigurationManager proxyConfigurationManager) {
        this.proxyConfigurationManager = proxyConfigurationManager;
    }

    public List<AppConfig.Proxy> getProxies() {
        return AppContext.get().getConfig().getProxies();
    }

    public List<AppConfig.Service> getServices() {
        return AppContext.get().getConfig().getServices();
    }

    public List<AppConfig.User> getUsers() {
        return AppContext.get().getConfig().getUsers();
    }

    public void validateAndAddOrUpdateProxy(AppConfig.Proxy proxy) {
        validateProxy(proxy);
        proxyConfigurationManager.addOrUpdateProxy(proxy);
    }

    public void validateAndAddOrUpdateService(AppConfig.Service service) {
        validateService(service);
        // Add logic to update services in ProxyConfigurationManager if needed
    }

    private void validateProxy(AppConfig.Proxy proxy) {
        if (proxy.getPath() == null || proxy.getPath().isEmpty()) {
            throw new JetProxyValidationException("Proxy path cannot be null or empty");
        }
        if (!proxy.getPath().startsWith("/")) {
            throw new JetProxyValidationException("Proxy path must start with '/'");
        }
        if (proxy.getService() == null || proxy.getService().isEmpty()) {
            throw new JetProxyValidationException("Proxy service cannot be null or empty");
        }

        List<AppConfig.Service> services = getServices();
        if (services.stream().noneMatch(service -> service.getName().equals(proxy.getService()))) {
            throw new JetProxyValidationException("Proxy service does not exist: " + proxy.getService());
        }
    }

    private void validateService(AppConfig.Service service) {
        if (service.getName() == null || service.getName().isEmpty()) {
            throw new JetProxyValidationException("Service name cannot be null or empty");
        }
        if (service.getUrl() == null || service.getUrl().isEmpty()) {
            throw new JetProxyValidationException("Service URL cannot be null or empty");
        }
        if (!service.getUrl().startsWith("http://") && !service.getUrl().startsWith("https://")) {
            throw new JetProxyValidationException("Service URL must start with 'http://' or 'https://'");
        }
    }
}
