package proxy.middleware.metric;

import com.timgroup.statsd.StatsDClient;

public class StatsDMetricsListener implements MetricsListener {

    private final StatsDClient statsDClient;

    public StatsDMetricsListener(StatsDClient statsDClient) {
        this.statsDClient = statsDClient;
    }

    @Override
    public void onProxyPathUsed(String path, int statusCode, long size) {
        statsDClient.incrementCounter("proxy.path." + path + "_" + statusCode);
        statsDClient.incrementCounter("http.status." + statusCode);
        statsDClient.recordGaugeValue("response.size." + path, size);
    }

}
