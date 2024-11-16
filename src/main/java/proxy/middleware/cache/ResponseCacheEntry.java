package proxy.middleware.cache;

import java.util.Map;

public class ResponseCacheEntry {
    private Map<String, String> headers;
    private String body;

    public ResponseCacheEntry(Map<String, String> headers, String body) {
        this.headers = headers;
        this.body = body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }
}
