package proxy.auth;

public class AuthProviderFactory {

    public static AuthProvider getAuthProvider(String providerType) {
        if ("basicAuth".equalsIgnoreCase(providerType)) {
            return new BasicAuthProvider();
        } else if ("firebaseAuth".equalsIgnoreCase(providerType)) {
            // return new FirebaseAuthProvider();
        }
        throw new IllegalArgumentException("Unknown auth provider: " + providerType);
    }
}
