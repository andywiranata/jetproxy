package io.jetproxy.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Duration;

public class DurationAdapter extends TypeAdapter<Duration> {

    @Override
    public void write(JsonWriter out, Duration value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.toString()); // Serialize as ISO-8601 string (e.g., "PT20S" for 20 seconds)
        }
    }

    @Override
    public Duration read(JsonReader in) throws IOException {
        String durationString = in.nextString();
        return Duration.parse(durationString); // Parse ISO-8601 string to Duration
    }
}
