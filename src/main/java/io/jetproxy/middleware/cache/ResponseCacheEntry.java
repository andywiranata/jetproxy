package io.jetproxy.middleware.cache;

import lombok.Getter;

import java.util.Map;

@Getter
public class ResponseCacheEntry {
    private Map<String, String> headers;
    private String body;

    public ResponseCacheEntry(Map<String, String> headers, String body) {
        this.headers = headers;
        this.body = body;
    }

}
