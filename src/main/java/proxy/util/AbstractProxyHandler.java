package proxy.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.brotli.dec.BrotliInputStream;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.config.AppContext;

import java.io.*;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public abstract class AbstractProxyHandler extends ProxyServlet.Transparent {

    private static final Logger logger = LoggerFactory.getLogger(AbstractProxyHandler.class);

    // Shared logic for checking the cache
    protected String getCachedResponse(HttpServletRequest request) {
        if (!request.getMethod().equalsIgnoreCase("GET")) {
            return null;
        }
        String path = RequestUtils.getFullPath(request);
        String method = request.getMethod();
        return AppContext
                .getInstance()
                .getCache()
                .get(String.format("%s__%s", method, path));
    }

    // Shared logic for caching the response
    protected void cacheResponseContent(HttpServletRequest request, String bodyContent) {
        if (!request.getMethod().equalsIgnoreCase("GET")) {
            return;
        }
        String path = RequestUtils.getFullPath(request);
        String method = request.getMethod();
        // TODO: Make the TTL configurable
        AppContext
                .getInstance()
                .getCache()
                .put(String.format("%s__%s", method, path),
                        bodyContent,
                        5000);

    }

    // Shared logic for sending cached responses
    protected void sendCachedResponse(HttpServletResponse response, String cachedResponse) {
        response.setContentType("application/json");
        try {
            response.getWriter().write(cachedResponse);
        } catch (IOException e) {
            logger.error("Error writing cached response: {}", e.getMessage());
        }
    }

    // Shared logic for decoding content streams
    protected InputStream decodeContentStream(InputStream inputStream, String contentEncoding)
            throws IOException {
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

    // Shared logic for determining if content is JSON
    protected boolean isJsonContent(String contentType) {
        return contentType != null && contentType.contains("application/json");
    }

    // Shared logic for reading streams as a string
    protected String readStreamAsString(InputStream inputStream, String contentType) throws IOException {
        String charset = getCharsetFromContentType(contentType);
        return new String(inputStream.readAllBytes(), Charset.forName(charset));
    }

    // Extract charset from content type, defaulting to UTF-8
    protected String getCharsetFromContentType(String contentType) {
        if (contentType.contains("charset=")) {
            return contentType.split("charset=")[1];
        }
        return "UTF-8"; // Default to UTF-8 if no charset is specified
    }

    // Shared error handling logic
    protected void handleError(HttpServletResponse response, Exception e) {
        logger.error("Error processing request: {}", e.getMessage());
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
