package io.jetproxy.service;

import io.jetproxy.context.AppConfig;
import io.jetproxy.context.AppContext;
import io.jetproxy.service.holder.ProxyConfigurationManager;
import io.jetproxy.util.AbstractJsonServlet;

import io.jetproxy.util.GsonFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;

public class AppConfigServlet extends AbstractJsonServlet<Object> {
    private final ProxyConfigurationManager proxyConfigurationManager;
    public AppConfigServlet(ProxyConfigurationManager proxyConfigurationManager) {
        this.proxyConfigurationManager = proxyConfigurationManager;
    }
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppConfig config = AppContext.get().getConfig();
        String path = req.getPathInfo();
        Object responseData;

        if (path == null || path.equals("/")) {
            // Handle root "/config" request
            sendJsonResponse(resp, "Please use /config/proxies, /config/services, or /config/users.",
                    HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        switch (path) {
            case "/proxies":
                responseData = config.getProxies();
                break;
            case "/services":
                responseData = config.getServices();
                break;
            case "/users":
                responseData = config.getUsers();
                break;
            default:
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                sendJsonResponse(resp, null, HttpServletResponse.SC_NOT_FOUND);
                return;
        }

        sendJsonResponse(resp, responseData, HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppConfig config = AppContext.get().getConfig();
        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            sendJsonResponse(resp, "Please use /config/proxies or /config/services.",
                    HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            switch (path) {
                case "/proxies":
                    handleUpdateProxies(req, resp);
                    break;
                case "/services":
                    handleUpdateServices(req, resp);
                    break;
                default:
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    sendJsonResponse(resp, null, HttpServletResponse.SC_NOT_FOUND);
                    break;
            }
        } catch (Exception e) {
            sendJsonResponse(resp, "Error processing request: " + e.getMessage(),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void handleUpdateProxies(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            AppConfig.Proxy newProxy = parseRequest(req, AppConfig.Proxy.class);
            // Add or update the proxy using SetupProxyHolder
            proxyConfigurationManager.addOrUpdateProxy(newProxy);
            sendJsonResponse(resp, "Proxy added or updated: " + newProxy.getPath(), HttpServletResponse.SC_OK);
        } catch (JsonSyntaxException e) {
            sendJsonResponse(resp, "Invalid proxy format: " + e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
        } catch (IllegalArgumentException e) {
            sendJsonResponse(resp, "Validation error: " + e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
        } catch (Exception e) {
            sendJsonResponse(resp, "Unknown error: " + e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);

        }
    }

    private void handleUpdateServices(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            AppConfig.Service newService = parseRequest(req, AppConfig.Service.class);

            // Update services in ConfigLoader (optional if dynamic services are required)
            // setupProxyHolder.addOrUpdateService(newService);

            sendJsonResponse(resp, "Service added or updated: " + newService.getName(), HttpServletResponse.SC_OK);
        } catch (JsonSyntaxException e) {
            sendJsonResponse(resp, "Invalid service format: " + e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
        } catch (IllegalArgumentException e) {
            sendJsonResponse(resp, "Validation error: " + e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
        }
    }


}
