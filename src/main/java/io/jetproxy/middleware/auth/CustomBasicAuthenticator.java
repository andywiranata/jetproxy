package io.jetproxy.middleware.auth;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.authentication.DeferredAuthentication;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;

public class CustomBasicAuthenticator extends LoginAuthenticator {
    private Charset _charset;

    public CustomBasicAuthenticator() {
    }

    public Charset getCharset() {
        return this._charset;
    }

    public void setCharset(Charset charset) {
        this._charset = charset;
    }

    public String getAuthMethod() {
        return "BASIC";
    }

    public Authentication validateRequest(ServletRequest req, ServletResponse res, boolean mandatory) throws ServerAuthException {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;
        String credentials = request.getHeader(HttpHeader.AUTHORIZATION.asString());

        if (credentials == null) {
            String rawQuery = request.getQueryString();
            if (rawQuery != null && rawQuery.contains("auth=")) {
                MultiMap<String> params = new MultiMap<>();
                UrlEncoded.decodeTo(rawQuery, params, StandardCharsets.UTF_8);
                credentials = params.getString("auth");
            }
        }

        try {
            if (!mandatory) {
                return new DeferredAuthentication(this);
            } else {
                if (credentials != null) {
                    int space = credentials.indexOf(32);
                    if (space > 0) {
                        String method = credentials.substring(0, space);
                        if ("basic".equalsIgnoreCase(method)) {
                            credentials = credentials.substring(space + 1);
                            Charset charset = this.getCharset();
                            if (charset == null) {
                                charset = StandardCharsets.ISO_8859_1;
                            }

                            credentials = new String(Base64.getDecoder().decode(credentials), charset);
                            int i = credentials.indexOf(58);
                            if (i > 0) {
                                String username = credentials.substring(0, i);
                                String password = credentials.substring(i + 1);
                                UserIdentity user = this.login(username, password, request);
                                if (user != null) {
                                    return new UserAuthentication(this.getAuthMethod(), user);
                                }
                            }
                        }
                    }
                }

                if (DeferredAuthentication.isDeferred(response)) {
                    return Authentication.UNAUTHENTICATED;
                } else {
                    String value = "basic realm=\"" + this._loginService.getName() + "\"";
                    Charset charset = this.getCharset();
                    if (charset != null) {
                        value = value + ", charset=\"" + charset.name() + "\"";
                    }

                    response.setHeader(HttpHeader.WWW_AUTHENTICATE.asString(), value);
                    response.sendError(401);
                    return Authentication.SEND_CONTINUE;
                }
            }
        } catch (IOException var14) {
            throw new ServerAuthException(var14);
        }
    }

    public boolean secureResponse(ServletRequest req, ServletResponse res, boolean mandatory, Authentication.User validatedUser) throws ServerAuthException {
        return true;
    }
}