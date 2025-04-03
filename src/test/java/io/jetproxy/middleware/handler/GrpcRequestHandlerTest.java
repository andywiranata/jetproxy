package io.jetproxy.middleware.handler;

import io.jetproxy.context.AppConfig;
import io.jetproxy.context.AppContext;
import io.jetproxy.util.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

public class GrpcRequestHandlerTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private AppContext context;
    private AppConfig.Proxy proxyRule;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        context = mock(AppContext.class);
        proxyRule = new AppConfig.Proxy();
        proxyRule.setService("exampleGrpcService");
    }

    @Test
    void should_do_nothing_if_not_grpc_service() throws Exception {
        when(context.isUseGrpcService("exampleGrpcService")).thenReturn(false);

        GrpcRequestHandler handler = new GrpcRequestHandler(proxyRule, context);
        handler.handle(request, response);

        verifyNoInteractions(request);
        verifyNoInteractions(response);
    }

    @Test
    void should_return_400_if_grpc_headers_missing() throws Exception {
        when(context.isUseGrpcService("exampleGrpcService")).thenReturn(true);
        when(request.getHeader(Constants.REQUEST_HEADER_GRPC_SERVICE_NAME)).thenReturn(null);
        when(request.getHeader(Constants.REQUEST_HEADER_GRPC_METHOD_NAME)).thenReturn(null);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        GrpcRequestHandler handler = new GrpcRequestHandler(proxyRule, context);
        handler.handle(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(response).setHeader(Constants.HEADER_X_PROXY_ERROR, Constants.ERROR_METHOD_NOT_ALLOWED);
        verify(response).setHeader(Constants.HEADER_X_PROXY_TYPE, Constants.TYPE_GRPC_SERV0CE_METHOD_NOT_FOUND);
        verify(response).flushBuffer();
    }

    @Test
    void should_set_grpc_attributes_if_headers_present() throws Exception {
        when(context.isUseGrpcService("exampleGrpcService")).thenReturn(true);
        when(request.getHeader(Constants.REQUEST_HEADER_GRPC_SERVICE_NAME)).thenReturn("com.example.MyService");
        when(request.getHeader(Constants.REQUEST_HEADER_GRPC_METHOD_NAME)).thenReturn("GetData");

        GrpcRequestHandler handler = new GrpcRequestHandler(proxyRule, context);
        handler.handle(request, response);

        verify(request).setAttribute(Constants.REQUEST_ATTRIBUTE_JETPROXY_GRPC_SERVICE_NAME, "com.example.MyService");
        verify(request).setAttribute(Constants.REQUEST_ATTRIBUTE_JETPROXY_GRPC_METHOD_NAME, "GetData");
        verify(response, never()).setStatus(anyInt());
        verify(response, never()).flushBuffer();
    }
}
