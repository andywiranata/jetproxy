package io.jetproxy;


import io.opentelemetry.api.GlobalOpenTelemetry;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.jetproxy.context.AppConfig;
import io.jetproxy.context.AppContext;

import java.util.concurrent.Executors;

public class MainProxy {
    private static final Logger logger = LoggerFactory.getLogger(MainProxy.class);

    public void start(String[] args) throws Exception {

        // Load the configuration file path from the environment
        String externalConfigPath = System.getenv("APP_CONFIG_PATH");
        if (externalConfigPath == null || externalConfigPath.isEmpty()) {
            // Read `--config=...` argument
            for (String arg : args) {
                if (arg.startsWith("--config=")) {
                    externalConfigPath = arg.substring("--config=".length());
                    break;
                }
            }
        }
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
        appContext.initializeServer(server);

        // Start the server
        server.start();
        logger.info("JetProxy server started on port {}", appConfig.getPort());

        server.join();
    }

    public static void main(String[] args) throws Exception {
        MainProxy app = new MainProxy();
        app.start(args);
    }
}
