package io.jetproxy.util;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class BufferedHttpServletRequestWrapper extends HttpServletRequestWrapper {
    private final byte[] body;

    public BufferedHttpServletRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
             ServletInputStream inputStream = request.getInputStream()) {
            byte[] chunk = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(chunk)) != -1) {
                buffer.write(chunk, 0, bytesRead);
            }
            body = buffer.toByteArray();
        }
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public int read() {
                return byteArrayInputStream.read();
            }

            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                throw new UnsupportedOperationException();
            }
        };
    }
    public byte[] getBodyAsByte() {
        return body;
    }
    public String getBodyAsString() {
        return new String(body, StandardCharsets.UTF_8);
    }
    public boolean isEmptyBody() {
        return body.length <= 0;
    }
}
