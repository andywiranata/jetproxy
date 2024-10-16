package util;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class AbstractJsonServlet <T> extends HttpServlet {
    protected void sendJsonResponse(HttpServletResponse resp, T data) throws IOException {
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().println(convertToJson(data));
    }

    private String convertToJson(T data) {
        // Use Gson to convert the object to JSON
        return new Gson().toJson(data);
    }
}
