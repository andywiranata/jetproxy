package proxy.middleware.metric;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public class CompositeMetricsListener implements MetricsListener {

    private final List<MetricsListener> listeners;

    public CompositeMetricsListener(List<MetricsListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public void captureMetricProxyResponse(HttpServletRequest request, HttpServletResponse response) {
        for (MetricsListener listener : listeners) {
            listener.captureMetricProxyResponse(request, response);
        }
    }

}