package io.jetproxy.service.appConfig.vo;

import io.jetproxy.context.AppConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UserVO {
    private String username;
    private String role;
    private String uuid;

    // Constructor to initialize UserVO with uuid
    public UserVO(AppConfig.User user) {
        this.username = user.getUsername();
        this.role = user.getRole();
        this.uuid = user.getUuid(); // Generate or get UUID
    }
}
