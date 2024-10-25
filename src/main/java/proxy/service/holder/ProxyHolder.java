package proxy.service.holder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.context.AppConfig;
import proxy.middleware.rule.RuleFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProxyHolder extends AbstractProxyHandler {
    private static final Logger logger = LoggerFactory.getLogger(ProxyHolder.class);

    public ProxyHolder(AppConfig.Service serviceName,
                       AppConfig.Proxy proxyRule) {
        this.configService = serviceName;
        this.proxyRule = proxyRule;
        this.ruleContext = RuleFactory.createRulesFromString(proxyRule.getRule());
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) {
        if (!ruleContext.evaluate(request)) {
            handleRuleNotAllowed(response);
            return;
        }

        if (isMethodNotAllowed(request)) {
            handleMethodNotAllowed(response);
            return;
        }
        String cachedResponse = getCachedResponse(request);
        if (cachedResponse != null) {
            sendCachedResponse(response, cachedResponse);
            return;
        }

        try {
            super.service(request, response);
            // Optionally cache the response here after processing
        } catch (ServletException | IOException e) {
            logger.error("Error occurred {}", e.getMessage());
            handleError(response, e);
        }
    }

    @Override
    protected void onServerResponseHeaders(HttpServletRequest clientRequest, HttpServletResponse proxyResponse, Response serverResponse) {
        super.onServerResponseHeaders(clientRequest, proxyResponse, serverResponse);
    }

    @Override
    protected void onProxyResponseFailure(HttpServletRequest clientRequest, HttpServletResponse proxyResponse, Response serverResponse, Throwable failure) {
        super.onProxyResponseFailure(clientRequest, proxyResponse, serverResponse, failure);
    }

    @Override
    protected void onResponseContent(HttpServletRequest request,
                                     HttpServletResponse response,
                                     Response proxyResponse,
                                     byte[] buffer,
                                     int offset,
                                     int length, Callback callback) {

        if (this.proxyRule.getTtl() > 0) {
            String contentType = proxyResponse.getHeaders().get("Content-Type");
            String contentEncoding = proxyResponse.getHeaders().get("Content-Encoding");
            try (InputStream decodedStream = decodeContentStream(new ByteArrayInputStream(buffer, offset, length), contentEncoding)) {
                if (isJsonContent(contentType)) {
                    String bodyContent = readStreamAsString(decodedStream, contentType);
                    cacheResponseContent(request, bodyContent);
                }
            } catch (IOException e) {
                logger.error("Error decode response content {}", e.getMessage());
            }
        }

        super.onResponseContent(request, response, proxyResponse, buffer, offset, length, callback);
    }

    @Override
    protected void onProxyResponseSuccess(HttpServletRequest clientRequest, HttpServletResponse proxyResponse, Response serverResponse) {
        super.onProxyResponseSuccess(clientRequest, proxyResponse, serverResponse);
    }
}
