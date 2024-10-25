package proxy.middleware.metric;

public interface MetricsListener {
    void onProxyPathUsed(String path, int statusCode, long size);

}
