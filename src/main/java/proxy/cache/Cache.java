package proxy.cache;

public interface Cache {
    String get(int key);
    void put(int key, String value);
}
