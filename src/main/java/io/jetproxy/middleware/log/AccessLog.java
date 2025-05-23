package io.jetproxy.middleware.log;

import io.jetproxy.context.AppContext;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import io.jetproxy.logger.DebugAwareLogger;

public class AccessLog implements RequestLog {
    private static final DebugAwareLogger logger = DebugAwareLogger.getLogger(AccessLog.class);
    @Override
    public void log(Request request, Response response) {
        logger.logRequest(request, response);
    }
}
