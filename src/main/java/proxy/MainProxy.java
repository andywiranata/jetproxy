package proxy;

import com.sun.tools.javac.Main;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.config.AppConfig;
import proxy.config.ConfigLoaderVO;
import proxy.service.ProxyHandler;

import java.util.List;

public class MainProxy {
    private static final Logger logger = LoggerFactory.getLogger(MainProxy.class);
    public MainProxy() {
    }
    public void start() throws Exception {
        // Load configuration from YAML file
        AppConfig config = ConfigLoaderVO.getConfig();
        // Create a Jetty server instance on port 8080
        Server server = new Server(config.getPort());
        // Create a ServletContextHandler for managing different context paths
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        // Dynamically add proxies based on the YAML configuration
        List<AppConfig.ProxyRule> proxies = config.getProxies();
        for (AppConfig.ProxyRule proxyRule : proxies) {
            ServletHolder proxyServlet = new ServletHolder(
                    new ProxyHandler(proxyRule.getTarget()));
            proxyServlet.setInitParameter("proxyTo", proxyRule.getTarget());
            proxyServlet.setInitParameter("prefix", proxyRule.getPath());
            context.addServlet(proxyServlet, proxyRule.getPath() + "/*");
            logger.info("Proxy added: " + proxyRule.getPath() + " -> " + proxyRule.getTarget());
        }

        server.setHandler(context);

        server.start();
        logger.info("Proxy server started on port {}", config.getPort());
        server.join();
    }

    public static void  main(String[] args) throws Exception {

//        ConfigLoaderStrategy configLoaderStrategy = new YamlConfigLoader();
        // Create the main application
        MainProxy app = new MainProxy();
        app.start();
    }

}
