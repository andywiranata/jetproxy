package io.jetproxy.middleware.grpc;

import lombok.Getter;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpVersion;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class MockResponse implements Response {
    private final int status;
    private final String reason;
    private final HttpFields headers;
    @Getter
    private final ByteBuffer body;

    public MockResponse(int status, String reason, Map<String, String> headersMap, String bodyContent) {
        this.status = status;
        this.reason = reason;
        // ✅ FIX: Use HttpFields.build() to create an immutable headers instance
        HttpFields.Mutable headersBuilder = HttpFields.build();
        headersMap.forEach((key, value) -> headersBuilder.put(key, value));
        this.headers = headersBuilder.asImmutable();

        this.body = ByteBuffer.wrap(bodyContent.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public String getReason() {
        return reason;
    }

    @Override
    public HttpFields getHeaders() {
        return headers;
    }

    @Override
    public boolean abort(Throwable cause) {
        return false;
    }

    @Override
    public Request getRequest() {
        return null;
    }

    @Override
    public <T extends ResponseListener> List<T> getListeners(Class<T> listenerClass) {
        return null;
    }

    @Override
    public HttpVersion getVersion() {
        return HttpVersion.HTTP_1_1;
    }

    // Factory methods for quick mock responses
    public static MockResponse createSuccessResponse(String body) {
        return new MockResponse(200, "OK", Map.of("Content-Type", "application/json"), body);
    }

    public static MockResponse createErrorResponse(int status, String message) {
        return new MockResponse(status, message, Map.of("Content-Type", "text/plain"), message);
    }
}
