package io.jetproxy.middleware.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomBasicAuthenticatorTest {

    @Test
    void shouldAuthenticateFromAuthorizationQueryParam() throws Exception {
        String username = "admin";
        String password = "secret";
        String basicToken = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);

        when(req.getHeader("Authorization")).thenReturn(null);
        when(req.getQueryString()).thenReturn("authorization=" + java.net.URLEncoder.encode(basicToken, "UTF-8"));

        CustomBasicAuthenticator authenticator = spy(new CustomBasicAuthenticator());
        authenticator.setCharset(StandardCharsets.UTF_8);

        UserIdentity mockUser = mock(UserIdentity.class);
        when(mockUser.getUserPrincipal()).thenReturn(() -> username);
        doReturn(mockUser).when(authenticator).login(eq(username), eq(password), eq(req));

        Authentication result = authenticator.validateRequest(req, res, true);

        assertInstanceOf(UserAuthentication.class, result);
        assertEquals(username, ((UserAuthentication) result).getUserIdentity().getUserPrincipal().getName());
    }

    @Test
    void shouldAuthenticateFromAuthorizationHeader() throws Exception {
        String username = "admin";
        String password = "secret";
        String token = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);

        when(req.getHeader("Authorization")).thenReturn(token);
        when(req.getQueryString()).thenReturn(null);

        CustomBasicAuthenticator authenticator = spy(new CustomBasicAuthenticator());
        authenticator.setCharset(StandardCharsets.UTF_8);

        UserIdentity mockUser = mock(UserIdentity.class);
        when(mockUser.getUserPrincipal()).thenReturn(() -> username);
        doReturn(mockUser).when(authenticator).login(eq(username), eq(password), eq(req));

        Authentication result = authenticator.validateRequest(req, res, true);

        assertInstanceOf(UserAuthentication.class, result);
        assertEquals(username, ((UserAuthentication) result).getUserIdentity().getUserPrincipal().getName());
    }

    @Test
    void shouldReturnSendContinueOnInvalidBase64() throws Exception {
        String invalidToken = "Basic YWRt"; // "adm" (no colon)

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);

        when(req.getHeader("Authorization")).thenReturn(null);
        when(req.getQueryString()).thenReturn("authorization=" + java.net.URLEncoder.encode(invalidToken, "UTF-8"));

        CustomBasicAuthenticator authenticator = spy(new CustomBasicAuthenticator());
        authenticator.setCharset(StandardCharsets.UTF_8);

        LoginService loginService = mock(LoginService.class);
        when(loginService.getName()).thenReturn("JetProxy");
        authenticator.setLoginService(loginService);

        Authentication result = authenticator.validateRequest(req, res, true);

        assertEquals(Authentication.SEND_CONTINUE, result);
    }

    @Test
    void shouldReturnSendContinueForWrongCredentials() throws Exception {
        String token = "Basic " + Base64.getEncoder().encodeToString("wrong:wrong".getBytes(StandardCharsets.UTF_8));

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);

        when(req.getHeader("Authorization")).thenReturn(null);
        when(req.getQueryString()).thenReturn("authorization=" + java.net.URLEncoder.encode(token, "UTF-8"));

        CustomBasicAuthenticator authenticator = spy(new CustomBasicAuthenticator());
        LoginService loginService = mock(LoginService.class);
        when(loginService.getName()).thenReturn("JetProxy");
        authenticator.setLoginService(loginService);

        doReturn(null).when(authenticator).login(any(), any(), any());

        Authentication result = authenticator.validateRequest(req, res, true);

        assertEquals(Authentication.SEND_CONTINUE, result);
    }

    @Test
    void shouldReturnSendContinueWhenQueryParamLacksBasicPrefix() throws Exception {
        String token = Base64.getEncoder().encodeToString("admin:secret".getBytes());

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);

        when(req.getHeader("Authorization")).thenReturn(null);
        when(req.getQueryString()).thenReturn("authorization=" + java.net.URLEncoder.encode(token, "UTF-8"));

        CustomBasicAuthenticator authenticator = new CustomBasicAuthenticator();
        LoginService loginService = mock(LoginService.class);
        when(loginService.getName()).thenReturn("JetProxy");
        authenticator.setLoginService(loginService);

        Authentication result = authenticator.validateRequest(req, res, true);

        assertEquals(Authentication.SEND_CONTINUE, result);
    }
}
