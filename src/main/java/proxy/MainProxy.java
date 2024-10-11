package proxy;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.config.AppConfig;
import proxy.config.ConfigLoaderVO;
import proxy.service.ProxyService;

public class MainProxy {
    private static final Logger logger = LoggerFactory.getLogger(MainProxy.class);

    public void start() throws Exception {
        // Load configuration from YAML file
        AppConfig config = ConfigLoaderVO.getConfig();

        // Create a Jetty server instance on the configured port
        Server server = new Server(config.getPort());

        // Create a ServletContextHandler for managing different context paths
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        // Set up proxies
        ProxyService proxyService = new ProxyService(config);
        proxyService.setupProxies(context);

        server.setHandler(context);
        server.start();
        logger.info("Proxy server started on port {}", config.getPort());
        server.join();
    }

    public static void main(String[] args) throws Exception {
        // Create the main application
        MainProxy app = new MainProxy();
        app.start();
    }
}
