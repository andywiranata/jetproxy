package io.jetproxy.service.appConfig.servlet;

import io.jetproxy.context.AppConfig;
import io.jetproxy.service.appConfig.service.AppConfigService;
import io.jetproxy.util.AbstractJsonServlet;
import io.jetproxy.util.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;

public class AppConfigServlet extends AbstractJsonServlet<Object> {
    private final AppConfigService appConfigService;

    public AppConfigServlet(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            sendJsonResponse(resp, new ApiResponse<>(false,
                            "Please use /config/proxies, /config/services, or /config/users.", null),
                    HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            Object data = switch (path) {
                case "/proxies" -> appConfigService.getProxies();
                case "/services" -> appConfigService.getServices();
                case "/users" -> appConfigService.getUsers();
                default -> throw new IllegalArgumentException("Invalid path: " + path);
            };

            sendJsonResponse(resp, new ApiResponse<>(true, "Success", data), HttpServletResponse.SC_OK);
        } catch (IllegalArgumentException e) {
            sendJsonResponse(resp, new ApiResponse<>(false, e.getMessage(), null), HttpServletResponse.SC_BAD_REQUEST);
        } catch (Exception e) {
            sendJsonResponse(resp, new ApiResponse<>(false, "Internal server error: " + e.getMessage(), null),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            sendJsonResponse(resp, new ApiResponse<>(false,
                            "Please use /config/proxies or /config/services.", null),
                    HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            switch (path) {
                case "/proxies" -> handleUpdateProxies(req, resp);
                case "/services" -> handleUpdateServices(req, resp);
                default -> sendJsonResponse(resp, new ApiResponse<>(false,
                        "Invalid path: " + path, null), HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            sendJsonResponse(resp, new ApiResponse<>(false,
                            "Internal server error: " + e.getMessage(), null),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void handleUpdateProxies(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            AppConfig.Proxy newProxy = parseRequest(req, AppConfig.Proxy.class);
            appConfigService.validateAndAddOrUpdateProxy(newProxy);

            sendJsonResponse(resp, new ApiResponse<>(true,
                            "Proxy added or updated: " + newProxy.getPath(), newProxy),
                    HttpServletResponse.SC_OK);
        } catch (JsonSyntaxException e) {
            sendJsonResponse(resp, new ApiResponse<>(false,
                            "Invalid proxy format: " + e.getMessage(), null),
                    HttpServletResponse.SC_BAD_REQUEST);
        } catch (IllegalArgumentException e) {
            sendJsonResponse(resp, new ApiResponse<>(false,
                            "Validation error: " + e.getMessage(), null),
                    HttpServletResponse.SC_BAD_REQUEST);
        } catch (Exception e) {
            sendJsonResponse(resp, new ApiResponse<>(false,
                            "Unknown error: " + e.getMessage(), null),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void handleUpdateServices(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            AppConfig.Service newService = parseRequest(req, AppConfig.Service.class);
            appConfigService.validateAndAddOrUpdateService(newService);

            sendJsonResponse(resp, new ApiResponse<>(true,
                            "Service added or updated: " + newService.getName(), newService),
                    HttpServletResponse.SC_OK);
        } catch (JsonSyntaxException e) {
            sendJsonResponse(resp, new ApiResponse<>(false,
                            "Invalid service format: " + e.getMessage(), null),
                    HttpServletResponse.SC_BAD_REQUEST);
        } catch (IllegalArgumentException e) {
            sendJsonResponse(resp, new ApiResponse<>(false,
                            "Validation error: " + e.getMessage(), null),
                    HttpServletResponse.SC_BAD_REQUEST);
        } catch (Exception e) {
            sendJsonResponse(resp, new ApiResponse<>(false,
                            "Unknown error: " + e.getMessage(), null),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}

