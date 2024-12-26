package io.jetproxy.service.appConfig.vo;

import io.jetproxy.context.AppConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class ServiceVO {
    private String name;
    private String url;
    private List<String> methods;
    private String role;
    private String healthcheck;
    private String uuid;

    // Constructor to initialize ServiceVO with uuid
    public ServiceVO(AppConfig.Service service) {
        this.name = service.getName();
        this.url = service.getUrl();
        this.methods = service.getMethods();
        this.role = service.getRole();
        this.healthcheck = service.getHealthcheck();
        this.uuid = service.getUuid(); // Generate or get UUID
    }
}
