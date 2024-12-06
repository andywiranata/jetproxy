package proxy.service.holder;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import proxy.context.AppConfig;

import java.util.List;

public class SetupCorsHolder {
    private final AppConfig appConfig;

    /**
     * Constructor to initialize with application configuration.
     *
     * @param appConfig The application configuration containing CORS settings.
     */
    public SetupCorsHolder(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * Creates and configures a FilterHolder for CORS.
     *
     * @return A configured FilterHolder instance for CORS.
     */
    public FilterHolder createCorsFilter() {
        // Retrieve CORS settings from AppConfig
        List<String> allowMethods = appConfig.getCorsFilter().getAccessControlAllowMethods();
        List<String> allowHeaders = appConfig.getCorsFilter().getAccessControlAllowHeaders();
        List<String> allowOrigins = appConfig.getCorsFilter().getAccessControlAllowOriginList();

        // Configure the CORS filter
        FilterHolder cors = new FilterHolder(CrossOriginFilter.class);
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, joinWithComma(allowOrigins));
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, joinWithComma(allowMethods));
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, joinWithComma(allowHeaders));
        cors.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "true"); // Allow credentials

        return cors;
    }

    /**
     * Joins a list of strings with a comma, handling single-element lists without extra formatting.
     *
     * @param list The list to process.
     * @return A string with the elements joined by a comma or the single element if the list has only one value.
     */
    private String joinWithComma(List<String> list) {
        if (list.size() == 1) {
            return list.get(0); // Return the single value directly
        }
        return String.join(", ", list); // Join multiple values with a comma and space
    }
}
