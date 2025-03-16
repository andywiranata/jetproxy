package io.jetproxy.middleware.auth;

import io.jetproxy.context.AppConfig;

import java.net.HttpURLConnection;
import java.util.Map;

class TestForwardAuthAuthenticator extends ForwardAuthAuthenticator {
    private final HttpURLConnection mockConnection;

    public TestForwardAuthAuthenticator(AppConfig.Middleware appMiddleware, HttpURLConnection mockConnection) {
        super(appMiddleware);
        this.mockConnection = mockConnection;
    }

    @Override
    protected HttpURLConnection performForwardAuthRequest(String authUrl, Map<String, String> headers) {
        return mockConnection;
    }
}
