package proxy.service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.brotli.dec.BrotliInputStream;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.util.Callback;
import proxy.cache.LRUCacheWithTTL;
import util.RequestUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class ProxyHolder extends ProxyServlet.Transparent {

    private final String target;
    private final LRUCacheWithTTL cache;


    public ProxyHolder(String target) {
        this.target = target;
        // TODO REFACTOR
        this.cache = new LRUCacheWithTTL(2, 10);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) {
        try {
            String requestUri = RequestUtils.getFullPath(request);

            String cachedResponse = cache.get(requestUri);
            if (cachedResponse != null) {
                // If cached response exists, return it
                response.getWriter().write(cachedResponse);
                return;
            }
            super.service(request, response);


            // Cache the response
            cache.put(requestUri, "hello", -1);

        } catch (ServletException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    protected void onServerResponseHeaders(HttpServletRequest clientRequest, HttpServletResponse proxyResponse, Response serverResponse) {
        super.onServerResponseHeaders(clientRequest, proxyResponse, serverResponse);
    }

    @Override
    protected void onResponseContent(HttpServletRequest request,
                                     HttpServletResponse response,
                                     Response proxyResponse,
                                     byte[] buffer, int offset, int length, Callback callback) {
        String contentType = proxyResponse.getHeaders().get("Content-Type");
        String contentEncoding = proxyResponse.getHeaders().get("Content-Encoding");
        String transferEncoding = proxyResponse.getHeaders().get("Transfer-Encoding");

        System.out.println("Content-Encoding: " + contentEncoding);
        System.out.println("Content-Length: " + proxyResponse.getHeaders().get("Content-Length"));
        System.out.println("Transfer-Encoding: " + transferEncoding);

        try {
            InputStream decodedStream;
            decodedStream = decodeContentStream(new ByteArrayInputStream(buffer, offset, length), contentEncoding);

            // If JSON, handle it
            if (contentType != null && contentType.contains("application/json")) {
                // Determine charset from Content-Type, fallback to UTF-8
                String charset = "UTF-8";  // Default to UTF-8 if no charset is specified
                if (contentType.contains("charset=")) {
                    charset = contentType.split("charset=")[1];
                }
                // Read the decoded content as a string
                String bodyContent = new String(decodedStream.readAllBytes(), Charset.forName(charset));
                System.out.println("Response Body: " + bodyContent);
            } else {
                System.out.println("Received non-textual content or unsupported encoding.");
            }
        } catch (IOException e) {
//            e.printStackTrace();
            System.out.println("Error processing response content.");
        }
        super.onResponseContent(request,
                response, proxyResponse,
                buffer, offset, length, callback);
    }

    @Override
    protected void onProxyResponseSuccess(HttpServletRequest clientRequest,
                                          HttpServletResponse proxyResponse,
                                          Response serverResponse) {

        super.onProxyResponseSuccess(clientRequest, proxyResponse, serverResponse);
    }

    private InputStream decodeContentStream(InputStream inputStream, String contentEncoding) throws IOException {
        if (contentEncoding == null) {
            // No encoding, return the original stream
            return inputStream;
        }
        switch (contentEncoding) {
            case "gzip":
                try {
                    return new GZIPInputStream(inputStream);
                } catch (IOException e) {
                    System.out.println("Error decompressing GZIP: " + e.getMessage());
                    return inputStream;
                }
            case "deflate":
                return new InflaterInputStream(inputStream);  // Try with ZLIB header
            case "br":
                return new BrotliInputStream(inputStream);  // Handles Brotli encoding
            default:
                throw new IOException("Unsupported Content-Encoding: " + contentEncoding);
        }
    }

}