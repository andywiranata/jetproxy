package proxy.service;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.config.AppConfig;
import proxy.config.ConfigLoader;

import java.util.List;

public class ProxyService {
    private static final String PROXY_TO = "proxyTo";
    private static final String PREFIX = "prefix";
    private static final String TIMEOUT = "timeout";
    private static final Logger logger = LoggerFactory.getLogger(ProxyService.class);
    private final AppConfig config;

    public ProxyService(AppConfig config) {
        this.config = config;
    }

    public void setupProxies(ServletContextHandler context) {
        List<AppConfig.Proxy> proxies = config.getProxies();
        for (AppConfig.Proxy proxyRule : proxies) {
            String targetServiceUrl = ConfigLoader.getServiceMap().get(proxyRule.getService());
            if (targetServiceUrl == null) {
                throw new IllegalArgumentException("Service URL not found for: " + proxyRule.getService());
            }
            ServletHolder proxyServlet = new ServletHolder(new ProxyHolder());
            proxyServlet.setInitParameter(PROXY_TO, targetServiceUrl);
            proxyServlet.setInitParameter(PREFIX, proxyRule.getPath());
            context.addServlet(proxyServlet, proxyRule.getPath() + "/*");
            proxyServlet.setInitParameter(TIMEOUT, String.valueOf(config.getDefaultTimeout()));
            logger.info("Proxy added: {} -> {}", proxyRule.getPath(), targetServiceUrl);
        }
    }
}

//public class MetricsProxyServletWithStrategy extends ProxyServlet.Transparent {
//
//    private final MetricsListener metricsListener;
//
//    public MetricsProxyServletWithStrategy(MetricsListener metricsListener) {
//        this.metricsListener = metricsListener;
//    }
//
//    @Override
//    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
//        String path = request.getRequestURI();
//        long timestamp = System.currentTimeMillis();
//
//        // Notify listener about proxy path usage
//        metricsListener.onProxyPathUsed(path, timestamp);
//
//        // Proxy the request
//        super.service(request, response);
//
//        // Notify listener about HTTP status and response size
//        int statusCode = response.getStatus();
//        metricsListener.onHttpStatusReturned(statusCode, timestamp);
//
//        long contentLength = response.getContentLengthLong();
//        metricsListener.onResponseSizeRecorded(path, contentLength, timestamp);
//    }
//}

