package proxy;

import io.opentelemetry.api.GlobalOpenTelemetry;
import jakarta.servlet.DispatcherType;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.context.AppContext;
import proxy.service.AppShutdownListener;
import proxy.service.CORSFilter;
import proxy.service.HealthCheckServlet;
import proxy.service.holder.SetupProxyHolder;
import proxy.service.StatisticServlet;

import java.util.EnumSet;
import java.util.concurrent.Executors;

public class MainProxy {
    private static final Logger logger = LoggerFactory.getLogger(MainProxy.class);

    public void start() throws Exception {

        String externalConfigPath = System.getenv("APP_CONFIG_PATH");
        GlobalOpenTelemetry.get();

        AppContext appContext = new AppContext.Builder()
                .withPathConfig(externalConfigPath)
                .build();

        Server server = new Server(AppContext.get().getConfig().getPort());
        server.addBean(Executors.newVirtualThreadPerTaskExecutor());

        // Create a ServletContextHandler for managing different context paths
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);

        // Register the AppShutdownListener programmatically
        context.addEventListener(new AppShutdownListener());

        context.setContextPath(appContext.getConfig().getRootPath());

        context.addFilter(CORSFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        // Set up proxies
        SetupProxyHolder proxyService = new SetupProxyHolder(appContext.getConfig());

        proxyService.setupProxies(server, context);

        context.addServlet(HealthCheckServlet.class, "/healthcheck");
        context.addServlet(StatisticServlet.class, "/stats");

        server.start();
        logger.info("Proxy server started on port {}",  appContext.getConfig().getPort());

        server.join();

    }

    public static void main(String[] args) throws Exception {
        // Create the main application
        MainProxy app = new MainProxy();
        app.start();
    }
}
