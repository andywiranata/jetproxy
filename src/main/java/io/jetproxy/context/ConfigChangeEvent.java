package io.jetproxy.context;

import io.jetproxy.context.AppConfig.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Contract for configuration change events with dynamic payloads for proxies and services.
 */
@Data
public class ConfigChangeEvent {
    private String senderId;       // ID of the instance sending the event
    private List<Proxy> proxies;  // List of proxy objects
    private List<Service> services; // List of service objects

    // Private constructor to enforce factory method usage
    private ConfigChangeEvent(String senderId, List<Proxy> proxies, List<Service> services) {
        this.senderId = senderId;
        this.proxies = proxies;
        this.services = services;
    }

    /**
     * Factory method to create a ConfigChangeEvent for proxies.
     *
     * @param proxies List of proxies being updated.
     * @return ConfigChangeEvent for proxies.
     */
    public static ConfigChangeEvent forProxies(List<Proxy> proxies) {
        return new ConfigChangeEvent(
                AppContext.get().getInstanceId(), // Get UUID from AppContext config
                proxies, new ArrayList<>()
        );
    }

    /**
     * Factory method to create a ConfigChangeEvent for services.
     *
     * @param services List of services being updated.
     * @return ConfigChangeEvent for services.
     */
    public static ConfigChangeEvent forServices(List<Service> services) {
        return new ConfigChangeEvent(
                AppContext.get().getConfig().getUuid(), // Get UUID from AppContext config
                new ArrayList<>(),
                services
        );
    }
}
