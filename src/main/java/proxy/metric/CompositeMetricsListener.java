package proxy.metric;

import java.util.List;

public class CompositeMetricsListener implements MetricsListener {

    private final List<MetricsListener> listeners;

    public CompositeMetricsListener(List<MetricsListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public void onProxyPathUsed(String path, int statusCode, long size, long timestamp) {
        for (MetricsListener listener : listeners) {
            listener.onProxyPathUsed(path, statusCode, size, timestamp);
        }
    }
}