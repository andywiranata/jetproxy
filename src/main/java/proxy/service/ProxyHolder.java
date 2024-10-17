package proxy.service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.brotli.dec.BrotliInputStream;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.config.AppContext;
import proxy.util.RequestUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class ProxyHolder extends ProxyServlet.Transparent {

    private static final Logger logger = LoggerFactory.getLogger(ProxyHolder.class);

    public ProxyHolder() {
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) {
        String requestUri = RequestUtils.getFullPath(request);
        String cachedResponse = AppContext.getInstance().getCache().get(requestUri);

        if (cachedResponse != null) {
            // TODO Cache Header
            sendCachedResponse(response, cachedResponse);
            return;
        }
        try {
            super.service(request, response);
            // Optionally cache the response here after processing
        } catch (ServletException | IOException e) {
            handleError(response, e);
        }
    }

    private void sendCachedResponse(HttpServletResponse response, String cachedResponse) {
        response.setContentType("application/json"); // Set content type to JSON
        try {
            response.getWriter().write(cachedResponse);
        } catch (IOException e) {
            logger.error("Error writing cached response: {}", e.getMessage());
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
    protected void onResponseContent(HttpServletRequest request, HttpServletResponse response, Response proxyResponse, byte[] buffer, int offset, int length, Callback callback) {
        String contentType = proxyResponse.getHeaders().get("Content-Type");
        String contentEncoding = proxyResponse.getHeaders().get("Content-Encoding");

        try (InputStream decodedStream = decodeContentStream(new ByteArrayInputStream(buffer, offset, length), contentEncoding)) {
            if (isJsonContent(contentType)) {
                String bodyContent = readStreamAsString(decodedStream, contentType);
                cacheResponseContent(RequestUtils.getFullPath(request), bodyContent);
            } else {
                logger.warn("Received non-textual content or unsupported encoding.");
            }
        } catch (IOException e) {
            logger.error("Error processing response content: {}", e.getMessage());
        }

        super.onResponseContent(request, response, proxyResponse, buffer, offset, length, callback);
    }

    @Override
    protected void onProxyResponseSuccess(HttpServletRequest clientRequest, HttpServletResponse proxyResponse, Response serverResponse) {
        super.onProxyResponseSuccess(clientRequest, proxyResponse, serverResponse);
    }

    private void cacheResponseContent(String key, String bodyContent) {
        // TODO configureable ttl
        AppContext.getInstance().getCache().put(key, bodyContent, 5000);
    }

    private InputStream decodeContentStream(InputStream inputStream, String contentEncoding) throws IOException {
        if (contentEncoding == null) {
            return inputStream; // No encoding, return original stream
        }
        return switch (contentEncoding) {
            case "gzip" -> new GZIPInputStream(inputStream);
            case "deflate" -> new InflaterInputStream(inputStream);  // Handles ZLIB header
            case "br" -> new BrotliInputStream(inputStream);  // Handles Brotli encoding
            default -> inputStream; // Unknown encoding, return original stream
        };
    }

    private boolean isJsonContent(String contentType) {
        return contentType != null && contentType.contains("application/json");
    }

    private String readStreamAsString(InputStream inputStream, String contentType) throws IOException {
        String charset = getCharsetFromContentType(contentType);
        return new String(inputStream.readAllBytes(), Charset.forName(charset));
    }

    private String getCharsetFromContentType(String contentType) {
        if (contentType.contains("charset=")) {
            return contentType.split("charset=")[1];
        }
        return "UTF-8"; // Default to UTF-8 if no charset is specified
    }

    private void handleError(HttpServletResponse response, Exception e) {
        logger.error("Error processing request: {}", e.getMessage());
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
