package io.jetproxy;

import io.jetproxy.service.holder.SetupCorsHolder;
import io.opentelemetry.api.GlobalOpenTelemetry;
import jakarta.servlet.DispatcherType;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.jetproxy.context.AppConfig;
import io.jetproxy.context.AppContext;
import io.jetproxy.service.AppShutdownListener;
import io.jetproxy.service.HealthCheckServlet;
import io.jetproxy.service.holder.SetupProxyHolder;
import io.jetproxy.service.StatisticServlet;

import java.util.EnumSet;
import java.util.List;
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

        // Initialize the server with the configured port
        Server server = new Server(appConfig.getPort());
        server.addBean(Executors.newVirtualThreadPerTaskExecutor());

        // Create a ServletContextHandler for managing different context paths
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath(appConfig.getRootPath());

        // Register the AppShutdownListener programmatically
        context.addEventListener(new AppShutdownListener());

        // Initialize and configure the CORS filter
        SetupCorsHolder corsFilterSetup = new SetupCorsHolder(appConfig);
        FilterHolder cors = corsFilterSetup.createCorsFilter();
        context.addFilter(cors, "/*", EnumSet.of(DispatcherType.REQUEST));

        // Set up proxies
        SetupProxyHolder proxyService = new SetupProxyHolder(appConfig);
        proxyService.setupProxies(server, context);

        // Add servlets for health check and statistics
        context.addServlet(HealthCheckServlet.class, "/healthcheck");
        context.addServlet(StatisticServlet.class, "/stats");

        // Gracefully shutdown the server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("Shutting down gracefully...");
                server.stop();
            } catch (Exception e) {
                logger.error("Error during shutdown", e);
            }
        }));

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
