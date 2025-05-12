package io.jetproxy.util;

/**
 * Utility for cleanly terminating JetProxy with helpful error messages.
 * Avoids stack traces for known validation or configuration errors.
 */
public class JetProxyExit {

    /**
     * Exits the application with an error message and status code 1.
     * Intended for fatal configuration issues.
     *
     * @param message Full error message (formatted).
     */
    public static void fatal(String message) {
        System.err.println("\n==== JetProxy Startup Failed ====\n");
        System.err.println(message);
        System.err.println("==================================\n");
        System.exit(1);
    }
}
