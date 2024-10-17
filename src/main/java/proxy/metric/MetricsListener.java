package proxy.metric;

public interface MetricsListener {
    void onProxyPathUsed(String path, long timestamp);
    void onHttpStatusReturned(int statusCode, long timestamp);
    void onResponseSizeRecorded(String path, long size, long timestamp);
}
