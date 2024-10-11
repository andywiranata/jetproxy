package proxy.service;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.config.AppConfig;
import proxy.config.ConfigLoader;

import java.util.List;

public class ProxyService {
    private static final Logger logger = LoggerFactory.getLogger(ProxyService.class);
    private final AppConfig config;

    public ProxyService(AppConfig config) {
        this.config = config;
    }

    public void setupProxies(ServletContextHandler context) {
        List<AppConfig.Proxy> proxies = config.getProxies();
        for (AppConfig.Proxy proxyRule : proxies) {
            String targetServiceUrl = ConfigLoader.getServiceMap().get(proxyRule.getService());
            String timeout = ConfigLoader.getServiceMap().get(proxyRule.getService());

            if (targetServiceUrl == null) {
                throw new IllegalArgumentException("Service URL not found for: " + proxyRule.getService());
            }
            ServletHolder proxyServlet = new ServletHolder(new ProxyHolder(targetServiceUrl));
            proxyServlet.setInitParameter("proxyTo", targetServiceUrl);
            proxyServlet.setInitParameter("prefix", proxyRule.getPath());
            context.addServlet(proxyServlet, proxyRule.getPath() + "/*");
            proxyServlet.setInitParameter("timeout", timeout);
            logger.info("Proxy added: {} -> {}", proxyRule.getPath(), targetServiceUrl);
        }
    }
}
