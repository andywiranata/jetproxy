package io.jetproxy.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.Duration;

public class GsonFactory {
    public static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(Duration.class, new DurationAdapter())
                .create();
    }
}
