package proxy.listener.metrics;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryMetricsListener implements MetricsListener {

    private final Map<String, List<Long>> proxyPathUsage = new ConcurrentHashMap<>();
    private final Map<Integer, List<Long>> httpStatusMetrics = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> byteSizeMetrics = new ConcurrentHashMap<>();

    @Override
    public void onProxyPathUsed(String path, long timestamp) {
        proxyPathUsage.computeIfAbsent(path, k -> new CopyOnWriteArrayList<>()).add(timestamp);
    }

    @Override
    public void onHttpStatusReturned(int statusCode, long timestamp) {
        httpStatusMetrics.computeIfAbsent(statusCode, k -> new CopyOnWriteArrayList<>()).add(timestamp);
    }

    @Override
    public void onResponseSizeRecorded(String path, long size, long timestamp) {
        byteSizeMetrics.computeIfAbsent(path, k -> new CopyOnWriteArrayList<>()).add(size);
    }
}
