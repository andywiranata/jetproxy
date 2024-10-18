package proxy.service;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.config.AppConfig;
import proxy.config.ConfigLoader;

import java.util.List;

public class SetupService {
    private static final String PROXY_TO = "proxyTo";
    private static final String PREFIX = "prefix";
    private static final String TIMEOUT = "timeout";
    private static final Logger logger = LoggerFactory.getLogger(SetupService.class);
    private final AppConfig config;

    public SetupService(AppConfig config) {
        this.config = config;
    }

    public void setupProxies(ServletContextHandler context) {
        List<AppConfig.Proxy> proxies = config.getProxies();
        for (AppConfig.Proxy proxyRule : proxies) {
            AppConfig.Service service = ConfigLoader.getServiceMap().get(proxyRule.getService());
            String targetServiceUrl = service.getUrl();
            if (targetServiceUrl == null) {
                throw new IllegalArgumentException("Service URL not found for: " + proxyRule.getService());
            }
            ServletHolder proxyServlet = new ServletHolder(new ProxyHolder(service));
            proxyServlet.setInitParameter(PROXY_TO, targetServiceUrl);
            proxyServlet.setInitParameter(PREFIX, proxyRule.getPath());
            context.addServlet(proxyServlet, proxyRule.getPath() + "/*");
            proxyServlet.setInitParameter(TIMEOUT, String.valueOf(config.getDefaultTimeout()));
            logger.info("Proxy added: {} -> {}", proxyRule.getPath(), targetServiceUrl);
        }
    }
}
