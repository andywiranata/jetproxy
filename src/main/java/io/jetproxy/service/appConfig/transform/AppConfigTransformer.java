package io.jetproxy.service.appConfig.transform;
import io.jetproxy.context.AppConfig;
import io.jetproxy.service.appConfig.vo.ProxyVO;
import io.jetproxy.service.appConfig.vo.ServiceVO;
import io.jetproxy.service.appConfig.vo.UserVO;

import java.util.List;
import java.util.stream.Collectors;

public class AppConfigTransformer {

    // Transform a single Proxy instance to ProxyVO
    public static ProxyVO toProxyVO(AppConfig.Proxy proxy) {
        if (proxy == null) {
            throw new IllegalArgumentException("Proxy cannot be null");
        }
        return new ProxyVO(proxy);
    }

    // Transform a list of Proxy instances to a list of ProxyVO instances
    public static List<ProxyVO> toProxyVOList(List<AppConfig.Proxy> proxies) {
        if (proxies == null) {
            throw new IllegalArgumentException("Proxies list cannot be null");
        }
        return proxies.stream()
                .map(AppConfigTransformer::toProxyVO)
                .collect(Collectors.toList());
    }

    // Transform a single Service instance to ServiceVO
    public static ServiceVO toServiceVO(AppConfig.Service service) {
        if (service == null) {
            throw new IllegalArgumentException("Service cannot be null");
        }
        return new ServiceVO(service);
    }

    // Transform a list of Service instances to a list of ServiceVO instances
    public static List<ServiceVO> toServiceVOList(List<AppConfig.Service> services) {
        if (services == null) {
            throw new IllegalArgumentException("Services list cannot be null");
        }
        return services.stream()
                .map(AppConfigTransformer::toServiceVO)
                .collect(Collectors.toList());
    }

    public static UserVO toUserVO(AppConfig.User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        return new UserVO(user);
    }

    // Transform a list of User instances to a list of UserVO instances
    public static List<UserVO> toUserVOList(List<AppConfig.User> users) {
        if (users == null) {
            throw new IllegalArgumentException("Users list cannot be null");
        }
        return users.stream()
                .map(AppConfigTransformer::toUserVO)
                .collect(Collectors.toList());
    }
}
