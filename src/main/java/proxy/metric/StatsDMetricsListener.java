package proxy.metric;

import com.timgroup.statsd.StatsDClient;

public class StatsDMetricsListener implements MetricsListener {

    private final StatsDClient statsDClient;

    public StatsDMetricsListener(StatsDClient statsDClient) {
        this.statsDClient = statsDClient;
    }

    @Override
    public void onProxyPathUsed(String path, int statusCode, long size, long timestamp) {
        statsDClient.incrementCounter("proxy.path." + path);
        statsDClient.incrementCounter("http.status." + statusCode);
        statsDClient.recordGaugeValue("response.size." + path, size);
    }

}
