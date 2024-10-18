package proxy.metric;

public interface MetricsListener {
    void onProxyPathUsed(String path, int statusCode, long size, long timestamp);

}
