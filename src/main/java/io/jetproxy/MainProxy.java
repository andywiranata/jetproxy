package io.jetproxy;

import io.jetproxy.service.appConfig.servlet.AppConfigServlet;
import io.jetproxy.service.appConfig.service.AppConfigService;
import io.jetproxy.service.holder.handler.CorsFilterHolderHandler;
import io.opentelemetry.api.GlobalOpenTelemetry;
import jakarta.servlet.DispatcherType;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.jetproxy.context.AppConfig;
import io.jetproxy.context.AppContext;
import io.jetproxy.service.AppShutdownListener;
import io.jetproxy.service.HealthCheckServlet;
import io.jetproxy.service.holder.ProxyConfigurationManager;
import io.jetproxy.service.StatisticServlet;

import java.util.EnumSet;
import java.util.concurrent.Executors;

public class MainProxy {
    private static final Logger logger = LoggerFactory.getLogger(MainProxy.class);

    public void start() throws Exception {

        // Load the configuration file path from the environment
        String externalConfigPath = System.getenv("APP_CONFIG_PATH");
        GlobalOpenTelemetry.get();

        // Build the application context
        AppContext appContext = new AppContext.Builder()
                .withPathConfig(externalConfigPath)
                .build();

        AppConfig appConfig = appContext.getConfig();
        ServletContextHandler context = appContext.getContextHandler();
        // Initialize the server with the configured port
        Server server = new Server(appConfig.getPort());
        server.addBean(Executors.newVirtualThreadPerTaskExecutor());

        // Set up proxies
        appContext.setupProxyHandler(server);
        // Add servlets for health check and statistics
        context.addServlet(HealthCheckServlet.class, "/healthcheck");
        context.addServlet(StatisticServlet.class, "/stats");

        ServletHolder configServletHolder = new ServletHolder(
                new AppConfigServlet(
                        new AppConfigService()));
        context.addServlet(configServletHolder, "/config/*");

        // Start the server
        server.start();
        logger.info("Proxy server started on port {}", appConfig.getPort());

        server.join();
    }

    public static void main(String[] args) throws Exception {
        // Create the main application
        MainProxy app = new MainProxy();
        app.start();
    }
}
