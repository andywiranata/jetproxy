package proxy.service;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import proxy.context.AppConfig;
import proxy.context.AppContext;
import proxy.middleware.cache.RedisPoolManager;
import proxy.util.AbstractJsonServlet;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class HealthCheckServlet extends AbstractJsonServlet<HealthCheckServlet.HealthCheckResponse> {
    private static final String STATUS_HEALTHY = "Healthy";
    private static final String STATUS_UNHEALTHY = "Unhealthy";
    private static final String STATUS_NOT_FOUND = "Not Found";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppConfig.Storage redisServer = AppContext.get().getConfig().getStorage();
        // List of destination servers to check
        Map<String, AppConfig.Service>  serviceMap = AppContext.get().getServiceMap();
        // Ping each server and collect statuses
        Map<String, String> serverStatuses = new HashMap<>();
        HealthCheckResponse response;

        serviceMap.forEach((serviceName, serviceConfig) -> {
            if (serviceConfig.getHealthcheck() != null) {
                String serverUrl = serviceConfig.getUrl() + serviceConfig.getHealthcheck();
                serverStatuses.put(serverUrl, pingServer(serverUrl));
            } else {
                serverStatuses.put(serviceConfig.getUrl(), STATUS_NOT_FOUND);
            }
        });

        String redisStatus = redisServer.hasRedisServer() ? pingRedis() : STATUS_NOT_FOUND;
        String overallStatus = redisStatus.equals(STATUS_HEALTHY) ? "UP" : "DOWN";
        int responseStatusCode = redisStatus.equals(STATUS_HEALTHY) ? HttpServletResponse.SC_OK : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

        response = new HealthCheckResponse(overallStatus, redisStatus, serverStatuses);
        sendJsonResponse(resp, response, responseStatusCode);
    }

    /**
     * Pings a server by sending a HEAD request and checking the response code.
     *
     * @param serverUrl the URL of the server to ping
     * @return "Healthy" if the server responds with a status code 200-399, otherwise "Unhealthy"
     */
    private String pingServer(String serverUrl) {
        try {
            URL url = new URL(serverUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(2000); // 2 seconds timeout
            connection.connect();

            int responseCode = connection.getResponseCode();
            return (responseCode >= 200 && responseCode < 400) ? STATUS_HEALTHY : STATUS_UNHEALTHY;
        } catch (Exception e) {
            return "Unhealthy";
        }
    }

    /**
     * Pings the Redis server using RedisPoolManager.
     *
     * @return "Healthy" if Redis is reachable, otherwise "Unhealthy"
     */
    private String pingRedis() {
        try (var jedis = RedisPoolManager.getPool().getResource()) {
            return "PONG".equals(jedis.ping()) ? STATUS_HEALTHY : STATUS_UNHEALTHY;
        } catch (Exception e) {
            return "Unhealthy";
        }
    }


    @Getter
    public static class HealthCheckResponse {
        private final String status;
        private final String redisStatus;
        private final Map<String, String> servers;

        public HealthCheckResponse(String status, String redisStatus, Map<String, String> servers) {
            this.status = status;
            this.redisStatus = redisStatus;
            this.servers = servers;
        }

    }
}
