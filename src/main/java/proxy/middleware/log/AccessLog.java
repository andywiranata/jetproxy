package proxy.middleware.log;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.context.AppContext;
import proxy.logger.DebugAwareLogger;

import java.text.SimpleDateFormat;
import java.util.Date;

public class AccessLog implements RequestLog {
    private static final DebugAwareLogger logger = DebugAwareLogger.getLogger(AccessLog.class);
    @Override
    public void log(Request request, Response response) {
        logger.logRequest(request, response);
    }
}
