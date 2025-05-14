package io.jetproxy.util;

/**
 * Utility class to build structured, developer-friendly error messages for JetProxy.
 * <p>
 * Intended to be used when throwing {@link io.jetproxy.exception.JetProxyValidationException}
 * to provide verbose guidance, example YAML, and documentation links.
 *
 * <pre>
 * Example:
 * throw new JetProxyValidationException(
 *     JetProxyErrorBuilder.error("No proxies defined")
 *         .hint("Add at least one proxy to your config.yaml")
 *         .example("proxies:\n  - path: /api\n    service: apiService")
 *         .tip("Ensure YAML indentation is correct.")
 *         .doc("core/routers")
 *         .build()
 * );
 * </pre>
 */
public class JetProxyErrorBuilder {

    private final StringBuilder message;

    /**
     * Private constructor to initialize the error builder with a title.
     *
     * @param title the main error summary, typically a one-line failure cause.
     */
    private JetProxyErrorBuilder(String title) {
        this.message = new StringBuilder();
        message.append("❌ ").append(title).append("\n");
    }

    /**
     * Starts building an error message with a given title.
     *
     * @param title a short description of the error
     * @return a new JetProxyErrorBuilder instance
     */
    public static JetProxyErrorBuilder error(String title) {
        return new JetProxyErrorBuilder(title);
    }

    /**
     * Adds a user-friendly hint to help resolve the issue.
     *
     * @param hint a helpful suggestion or next step
     * @return the current builder instance
     */
    public JetProxyErrorBuilder hint(String hint) {
        message.append("👉 ").append(hint).append("\n");
        return this;
    }

    /**
     * Includes a YAML or config snippet as an example.
     *
     * @param yamlBlock a multi-line config example
     * @return the current builder instance
     */
    public JetProxyErrorBuilder example(String yamlBlock) {
        message.append("📄 Example:\n").append(yamlBlock).append("\n\n");
        return this;
    }

    /**
     * Adds general troubleshooting tips or multi-line notes.
     *
     * @param tips the tips to append
     * @return the current builder instance
     */
    public JetProxyErrorBuilder tip(String tips) {
        message.append(tips).append("\n\n");
        return this;
    }

    /**
     * Appends a documentation link based on the relative doc path.
     *
     * @param docPath path relative to the base documentation URL
     * @return the current builder instance
     */
    public JetProxyErrorBuilder doc(String docPath) {
        String fullLink = "📚 Read more: https://jetproxy.andywiranata.me/docs/" + docPath;
        message.append(fullLink).append("\n");
        return this;
    }

    /**
     * Builds the complete message string.
     *
     * @return the formatted error message
     */
    public String build() {
        return message.toString();
    }
}
