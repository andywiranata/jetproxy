package proxy.metric;

import com.timgroup.statsd.StatsDClient;

public class StatsDMetricsListener implements MetricsListener {

    private final StatsDClient statsDClient;

    public StatsDMetricsListener(StatsDClient statsDClient) {
        this.statsDClient = statsDClient;
    }

    @Override
    public void onProxyPathUsed(String path, long timestamp) {
        statsDClient.incrementCounter("proxy.path." + path);
    }

    @Override
    public void onHttpStatusReturned(int statusCode, long timestamp) {
        statsDClient.incrementCounter("http.status." + statusCode);
    }

    @Override
    public void onResponseSizeRecorded(String path, long size, long timestamp) {
        statsDClient.recordGaugeValue("response.size." + path, size);
    }
}
