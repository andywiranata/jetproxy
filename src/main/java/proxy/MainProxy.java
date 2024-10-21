package proxy;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.context.AppContext;
import proxy.service.HealthCheckServlet;
import proxy.service.holder.SetupProxyHolder;
import proxy.service.StatisticServlet;

public class MainProxy {
    private static final Logger logger = LoggerFactory.getLogger(MainProxy.class);

    public void start() throws Exception {
        AppContext appContext = new AppContext.Builder()
                .withMaxSize(10000) // Optional: Set max size
                .withMaxHeapMemory(50 * 1024 * 1024) // Optional: Set max heap memory
                .withPathConfig("config.yaml")
                .build();

        Server server = new Server(AppContext.getInstance().getConfig().getPort());

        // Create a ServletContextHandler for managing different context paths
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
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
