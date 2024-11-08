package proxy.middleware.auth;

public class AuthProviderFactory {

    public static AuthProvider getAuthProvider(String providerType) {
        if ("basicAuth".equalsIgnoreCase(providerType)) {
            return new BasicAuthProvider();
        } else if ("forwardAuth".equalsIgnoreCase(providerType)) {
             return new ForwardAuthProvider();
        }
        throw new IllegalArgumentException("Unknown auth provider: " + providerType);
    }
}
