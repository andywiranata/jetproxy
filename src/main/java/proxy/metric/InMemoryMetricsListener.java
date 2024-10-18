package proxy.metric;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryMetricsListener implements MetricsListener {
    public enum MetricType {
        SUCCESS,
        WARNING,
        ERROR
    }

    private final ConcurrentHashMap<String, MetricData> metrics = new ConcurrentHashMap<>();

    public static class MetricData {
        private final AtomicInteger success = new AtomicInteger();
        private final AtomicInteger warnings = new AtomicInteger();
        private final AtomicInteger errors = new AtomicInteger();

        public void increment(MetricType type) {
            switch (type) {
                case SUCCESS:
                    success.incrementAndGet();
                    break;
                case WARNING:
                    warnings.incrementAndGet();
                    break;
                case ERROR:
                    errors.incrementAndGet();
                    break;
            }
        }

        public int getSuccess() {
            return success.get();
        }

        public int getWarnings() {
            return warnings.get();
        }

        public int getErrors() {
            return errors.get();
        }

        public int getTotal() {
            return success.get() + warnings.get() + errors.get();
        }
    }

    @Override
    public void onProxyPathUsed(String path, int statusCode, long size, long timestamp) {
        metrics.computeIfAbsent(path, k -> new MetricData()).increment(MetricType.SUCCESS);
    }

}

