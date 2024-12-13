package io.jetproxy.middleware.metric;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface MetricsListener {
    void captureMetricProxyResponse(
            HttpServletRequest request, HttpServletResponse response);

}
