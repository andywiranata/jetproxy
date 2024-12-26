package io.jetproxy.service.appConfig.vo;

import io.jetproxy.context.AppConfig;
import io.jetproxy.context.AppContext;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ProxyVO {
    private String path;
    private String service;
    private String uuid;
    private long ttl;
    private AppConfig.Middleware middleware;

    // Constructor to initialize ProxyVO with uuid
    public ProxyVO(AppConfig.Proxy proxy) {
        this.path = proxy.getPath();
        this.service = proxy.getService();
        this.ttl = proxy.getTtl();
        this.middleware = proxy.getMiddleware();
        this.uuid = proxy.getUuid(); // Generate or get UUID
    }
}
