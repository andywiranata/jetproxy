package io.jetproxy.middleware.auth;

import io.jetproxy.context.AppConfig;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

class TestForwardAuthAuthenticator extends ForwardAuthAuthenticator {
    private final HttpURLConnection mockConnection;
    private boolean throwException = false;

    public TestForwardAuthAuthenticator(AppConfig.Middleware appMiddleware, HttpURLConnection mockConnection) {
        super(appMiddleware);
        this.mockConnection = mockConnection;
    }

    public void setThrowException(boolean throwException) {
        this.throwException = throwException;
    }

    @Override
    protected HttpURLConnection performForwardAuthRequest(String authUrl, Map<String, String> headers) throws IOException {
        if (throwException) {
            throw new IOException("Connection failed");
        }
        return mockConnection;
    }
}
