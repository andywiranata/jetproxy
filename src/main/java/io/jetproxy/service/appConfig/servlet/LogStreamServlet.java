package io.jetproxy.service.appConfig.servlet;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LogStreamServlet extends HttpServlet {
    private static final List<PrintWriter> clients = new CopyOnWriteArrayList<>();

    public static void broadcast(String log) {
        for (PrintWriter writer : clients) {
            try {
                writer.write("data: " + log + "\n\n");
                writer.flush();
            } catch (Exception e) {
                clients.remove(writer);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/event-stream");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Connection", "keep-alive");

        PrintWriter writer = resp.getWriter();
        clients.add(writer);

        // Send welcome message once
        writer.write("data: Connected to JetProxy logs\n\n");
        writer.flush();

        // Keep connection open — do nothing else, let broadcast() send real logs
        // Do not return or close the stream
        // ✅ Keep the request alive using async context
        final AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(0); // Never timeout
    }
}
