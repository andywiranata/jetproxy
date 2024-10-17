package proxy.metric;

import java.util.List;

public class CompositeMetricsListener implements MetricsListener {

    private final List<MetricsListener> listeners;

    public CompositeMetricsListener(List<MetricsListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public void onProxyPathUsed(String path, long timestamp) {
        for (MetricsListener listener : listeners) {
            listener.onProxyPathUsed(path, timestamp);
        }
    }

    @Override
    public void onHttpStatusReturned(int statusCode, long timestamp) {
        for (MetricsListener listener : listeners) {
            listener.onHttpStatusReturned(statusCode, timestamp);
        }
    }

    @Override
    public void onResponseSizeRecorded(String path, long size, long timestamp) {
        for (MetricsListener listener : listeners) {
            listener.onResponseSizeRecorded(path, size, timestamp);
        }
    }
}