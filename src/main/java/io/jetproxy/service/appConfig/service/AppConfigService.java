package io.jetproxy.service.appConfig.service;

import io.jetproxy.context.AppConfig;
import io.jetproxy.context.AppContext;
import io.jetproxy.context.ConfigChangeEvent;
import io.jetproxy.context.ConfigLoader;
import io.jetproxy.exception.JetProxyValidationException;
import io.jetproxy.service.appConfig.transform.AppConfigTransformer;
import io.jetproxy.service.appConfig.vo.ProxyVO;
import io.jetproxy.service.appConfig.vo.ServiceVO;
import io.jetproxy.service.appConfig.vo.UserVO;
import io.jetproxy.service.holder.ProxyConfigurationManager;

import java.util.List;

public class AppConfigService {
    public AppConfig getConfig() {
        return AppContext.get().getConfig();
    }
    public List<ProxyVO> getProxies() {
        return AppConfigTransformer.toProxyVOList(AppContext.get().getConfig().getProxies());
    }

    public List<ServiceVO> getServices() {
        return AppConfigTransformer.toServiceVOList(AppContext.get().getConfig().getServices());
    }

    public List<UserVO> getUsers() {
        return AppConfigTransformer.toUserVOList(AppContext.get().getConfig().getUsers());
    }

    public void validateAndAddOrUpdateProxy(AppConfig.Proxy proxy) {
        AppContext.get().getProxyConfigurationManager().addOrUpdateProxy(proxy);
        AppContext.get().publishConfigChangeEvent(ConfigChangeEvent.forProxies(List.of(proxy)));

    }

    public void validateAndAddOrUpdateService(AppConfig.Service service) {
        // Add logic to update services in ProxyConfigurationManager if needed
        ConfigLoader.addOrUpdateServices(List.of(service));
        AppContext.get().publishConfigChangeEvent(ConfigChangeEvent.forServices(List.of(service)));

    }

}
