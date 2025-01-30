package io.jetproxy.middleware.grpc;

import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpVersion;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GrpcResponse implements Response {
    private final int status;
    private final Map<String, String> headers;

    public GrpcResponse(int status, Map<String, String> headers) {
        this.status = status;
        this.headers = headers;
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
        return null;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public String getReason() {
        return null;
    }

    @Override
    public HttpFields getHeaders() {
        return null;
    }

    @Override
    public boolean abort(Throwable cause) {
        return false;
    }

    @Override
    public String toString() {
        return "MockJettyResponse{status=" + status + ", headers=" + headers + "}";
    }

    // Other methods in the Response interface can return default or empty values
}