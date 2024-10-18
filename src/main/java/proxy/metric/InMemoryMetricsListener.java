package proxy.metric;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.service.ProxyHolder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryMetricsListener implements MetricsListener {
    private static final Logger logger = LoggerFactory.getLogger(InMemoryMetricsListener.class);
    private final ConcurrentHashMap<String, MetricData> metrics = new ConcurrentHashMap<>();
    public static class MetricData {
        private final AtomicInteger success2xx = new AtomicInteger();
        private final AtomicInteger clientError4xx = new AtomicInteger();
        private final AtomicInteger serverError5xx = new AtomicInteger();
        private final AtomicInteger otherCodes = new AtomicInteger();

        public void increment(int statusCode) {
            if (statusCode >= 200 && statusCode < 300) {
                success2xx.incrementAndGet();
            } else if (statusCode >= 400 && statusCode < 500) {
                clientError4xx.incrementAndGet();
            } else if (statusCode >= 500 && statusCode < 600) {
                serverError5xx.incrementAndGet();
            } else {
                otherCodes.incrementAndGet();
            }
        }

        public int getSuccess2xx() {
            return success2xx.get();
        }

        public int getClientError4xx() {
            return clientError4xx.get();
        }

        public int getServerError5xx() {
            return serverError5xx.get();
        }

        public int getOtherCodes() {
            return otherCodes.get();
        }

        public int getTotal() {
            return success2xx.get() + clientError4xx.get() + serverError5xx.get() + otherCodes.get();
        }
    }

    @Override
    public void onProxyPathUsed(String path, int statusCode, long size) {
        metrics.computeIfAbsent(path, k -> new MetricData()).increment(statusCode);
        logger.info("Stats metric {} 2xx:{} 4xx:{} 5xx:{}",
                path,
                metrics.get(path).success2xx,
                metrics.get(path).clientError4xx,
                metrics.get(path).serverError5xx);
    }

    // Optional: You can add a method to get the metrics data for a specific path
    public MetricData getMetricsForPath(String path) {
        return metrics.get(path);
    }
}
