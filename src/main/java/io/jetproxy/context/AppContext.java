package io.jetproxy.context;

import com.google.gson.Gson;
import io.jetproxy.middleware.cache.Cache;
import io.jetproxy.middleware.cache.CacheFactory;
import io.jetproxy.middleware.cache.RedisPoolManager;
import io.jetproxy.middleware.metric.MetricsListener;
import io.jetproxy.middleware.metric.MetricsListenerFactory;
import io.jetproxy.service.AppShutdownListener;
import io.jetproxy.service.holder.ProxyConfigurationManager;
import io.jetproxy.middleware.handler.CorsFilterHolderHandler;
import io.jetproxy.util.GsonFactory;
import jakarta.servlet.DispatcherType;
import lombok.Getter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPubSub;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Getter
public class AppContext {
    private final String instanceId;
    private static final Logger logger = LoggerFactory.getLogger(AppContext.class);
    private static volatile AppContext instance;
    private final AppConfig config;
    private final Cache cache;
    private final MetricsListener metricsListener;
    private final boolean debugMode;
    public final Gson gson;
    private final ServletContextHandler contextHandler;
    private final ProxyConfigurationManager proxyConfigurationManager;
    private static boolean gracefullyShutdownAllowed = true;

    private static final List<Consumer<ConfigChangeEvent>> eventSubscribers = new CopyOnWriteArrayList<>();

    private AppContext(Builder builder) {
        this.config = ConfigLoader.getConfig(builder.pathConfigYaml);
        RedisPoolManager.initializePool(this.config.getStorage().getRedis());
        this.cache = CacheFactory.createCache(this.config);
        this.metricsListener = MetricsListenerFactory.createMetricsListener(this.config);
        this.debugMode = this.config.isDebugMode();
        this.gson = GsonFactory.createGson();
        this.contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        this.proxyConfigurationManager = new ProxyConfigurationManager(this.config, this.contextHandler);
        this.instanceId = this.config.getUuid();
    }

    public static AppContext get() {
        if (instance == null) {
            synchronized (AppContext.class) {
                if (instance == null) {
                    instance = new Builder().build();
                }
            }
        }
        return instance;
    }

    public void initializeServer(Server server) {
        // Initialize and configure the CORS filter
        CorsFilterHolderHandler corsFilterSetup = new CorsFilterHolderHandler(this.config);
        FilterHolder cors = corsFilterSetup.createCorsFilter();

        this.contextHandler.setContextPath(this.config.getRootPath());
        this.contextHandler.addEventListener(new AppShutdownListener());
        this.contextHandler.addFilter(cors, "/*", EnumSet.of(DispatcherType.REQUEST));
        this.proxyConfigurationManager.setupProxiesAndAdminApi(server, this.contextHandler);

        startRedisSubscription();

    }

    public Map<String, AppConfig.Service> getServiceMap() {
        return ConfigLoader.getServiceMap();
    }

    public void preventGracefullyShutdown() {
        gracefullyShutdownAllowed = false;
    }

    public void allowGracefullyShutdown() {
        gracefullyShutdownAllowed = true;
    }

    public boolean isGracefullyShutdownAllowed() {
        return gracefullyShutdownAllowed;
    }

    /**
     * Publish a configuration change event to Redis and notify local subscribers.
     *
     * @param event The configuration change event.
     */
    public void publishConfigChangeEvent(ConfigChangeEvent event) {
        if (!this.getConfig().getStorage().hasRedisServer()) {
            logger.warn("Cannot publish configuration change event: Redis server is not enabled or configured in the application settings.");
            return;
        }
        try {
            String message = this.gson.toJson(event);
            RedisPoolManager.publish(RedisPoolManager.CHANNEL_CONFIG_CHANGE, message);
            logger.info("Published configuration change event to Redis: {}", message);
        } catch (Exception e) {
            logger.error("Failed to publish configuration change event to Redis. Ensure the Redis server is running and reachable. Error: {}", e.getMessage(), e);
        }
    }

    /**
     * Start a Redis subscription to listen for configuration change events.
     */
    private void startRedisSubscription() {
        if (!this.getConfig().getStorage().hasRedisServer()) {
            logger.warn("Cannot start Redis subscription: Redis server is not enabled or configured in the application settings.");
            return;
        }
        new Thread(() -> {
            try {
                RedisPoolManager.subscribe(RedisPoolManager.CHANNEL_CONFIG_CHANGE, new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        if (RedisPoolManager.CHANNEL_CONFIG_CHANGE.equals(channel)) {
                            ConfigChangeEvent event = gson.fromJson(message, ConfigChangeEvent.class);
                            if (!event.getSenderId().equals(instanceId)) {
                                logger.info("Received configuration change event from Redis: {}", message);
                                try {
                                    if (event.getServices() != null && !event.getServices().isEmpty()) {
                                        ConfigLoader.addOrUpdateServices(event.getServices());
                                    }
                                    if (event.getProxies() != null && !event.getProxies().isEmpty()) {
                                        proxyConfigurationManager.addOrUpdateProxy(event.getProxies().get(0));
                                    }
                                } catch (Exception e) {
                                    logger.error("Failed to apply configuration change from Redis: {}", e.getMessage(), e);
                                }
                            }
                        }
                    }
                });
            } catch (Exception e) {
                logger.error("Failed to start Redis subscription. Ensure the Redis server is running and reachable. Error: {}", e.getMessage(), e);
            }
        }).start();
    }


    public static class Builder {
        private String pathConfigYaml;

        public Builder withPathConfig(String pathConfigYaml) {
            if (pathConfigYaml == null || pathConfigYaml.isEmpty()) {
                this.pathConfigYaml = "config.yaml";
                return this;
            }
            this.pathConfigYaml = pathConfigYaml;
            return this;
        }

        public AppContext build() {
            return new AppContext(this);
        }
    }
}
