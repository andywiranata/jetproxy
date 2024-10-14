package util;


import jakarta.servlet.http.HttpServletRequest;

public class RequestUtils {
    public static String getFullPath(HttpServletRequest request) {
        String uri = request.getRequestURI();        // e.g., /example
        String queryString = request.getQueryString(); // e.g., params=1

        if (queryString != null) {
            return uri + "?" + queryString;          // Combine the URI with the query string
        } else {
            return uri;                               // If there is no query string, just return the URI
        }
    }
}