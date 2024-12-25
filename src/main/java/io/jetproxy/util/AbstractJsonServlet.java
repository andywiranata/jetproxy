package io.jetproxy.util;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;

public class AbstractJsonServlet <T> extends HttpServlet {
    protected void sendJsonResponse(HttpServletResponse resp, T data) throws IOException {
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().println(convertToJson(data));
    }

    protected void sendJsonResponse(HttpServletResponse resp, T data, int status) throws IOException {
        resp.setContentType("application/json");
        resp.setStatus(status);
        resp.getWriter().println(convertToJson(data));
    }

    protected String convertToJson(T data) {
        // Use Gson to convert the object to JSON
        return GsonFactory.createGson().toJson(data);
    }
    protected <T> T parseRequest(HttpServletRequest req, Class<T> clazz) throws IOException {
        StringBuilder requestBody = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
        }
        return GsonFactory.createGson().fromJson(requestBody.toString(), clazz);

    }

}
