package io.jetproxy;


import io.jetproxy.middleware.auth.jwk.validator.FirebaseJwtValidator;
import io.jetproxy.service.appConfig.servlet.AppConfigServlet;
import io.jetproxy.service.appConfig.service.AppConfigService;
import io.jsonwebtoken.Claims;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.jetproxy.context.AppConfig;
import io.jetproxy.context.AppContext;
import io.jetproxy.service.HealthCheckServlet;
import io.jetproxy.service.StatisticServlet;

import java.util.concurrent.Executors;

public class MainProxy {
    private static final Logger logger = LoggerFactory.getLogger(MainProxy.class);

    public void start() throws Exception {

        // Load the configuration file path from the environment
        String externalConfigPath = System.getenv("APP_CONFIG_PATH");
        GlobalOpenTelemetry.get();

        // Build the application context
        AppContext appContext = new AppContext.Builder()
                .withPathConfig(externalConfigPath)
                .build();

        AppConfig appConfig = appContext.getConfig();
        ServletContextHandler context = appContext.getContextHandler();
        // Initialize the server with the configured port
        Server server = new Server(appConfig.getPort());
        server.addBean(Executors.newVirtualThreadPerTaskExecutor());
        appContext.initializeServer(server);
        // Add servlets for health check and statistics
        context.addServlet(HealthCheckServlet.class, "/healthcheck");
        context.addServlet(StatisticServlet.class, "/stats");

        ServletHolder configServletHolder = new ServletHolder(
                new AppConfigServlet(
                        new AppConfigService()));
        context.addServlet(configServletHolder, "/admin/config/*");

        // Start the server
        server.start();
        logger.info("JetProxy server started on port {}", appConfig.getPort());

        server.join();
    }

//        public static void main(String[] args) {
//            try {
//                // Auth0 Example
////                Auth0JwtValidator auth0Validator = new Auth0JwtValidator(
////                        "https://your-tenant.auth0.com/.well-known/jwks.json",
////                        "https://your-tenant.auth0.com/",
////                        "your-client-id"
////                );
////                Claims auth0Claims = auth0Validator.validateToken("your-auth0-jwt");
////                System.out.println("Auth0 Claims: " + auth0Claims);
//
//                // Firebase Example
//                FirebaseJwtValidator firebaseValidator = new FirebaseJwtValidator(
//                        "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com",
//                        "https://securetoken.google.com/utility-app-90288",
//                        "utility-app-90288"
//                );
//                Claims firebaseClaims = firebaseValidator.validateToken("eyJhbGciOiJSUzI1NiIsImtpZCI6ImE3MWI1MTU1MmI0ODA5OWNkMGFkN2Y5YmZlNGViODZiMDM5NmUxZDEiLCJ0eXAiOiJKV1QifQ.eyJuYW1lIjoiQW5keSBXaXJhbmF0YSIsInBpY3R1cmUiOiJodHRwczovL2xoMy5nb29nbGV1c2VyY29udGVudC5jb20vYS9BQ2c4b2NKMWFOcmdBZENJSTlBS3lrY25vZXZrU0Q0YVBoZHlhYTRTTGpXLTl4T2JFanZhd1E4PXM5Ni1jIiwiaXNzIjoiaHR0cHM6Ly9zZWN1cmV0b2tlbi5nb29nbGUuY29tL3V0aWxpdHktYXBwLTkwMjg4IiwiYXVkIjoidXRpbGl0eS1hcHAtOTAyODgiLCJhdXRoX3RpbWUiOjE3MjY0ODkyOTUsInVzZXJfaWQiOiIyWE9xUXNoWHZRVDBSYUhablRjYXF3V01mSWYxIiwic3ViIjoiMlhPcVFzaFh2UVQwUmFIWm5UY2Fxd1dNZklmMSIsImlhdCI6MTczNTU2MTAxNiwiZXhwIjoxNzM1NTY0NjE2LCJlbWFpbCI6ImFuZHl3aXJhbmF0YXdpamF5YUBnbWFpbC5jb20iLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiZmlyZWJhc2UiOnsiaWRlbnRpdGllcyI6eyJnb29nbGUuY29tIjpbIjEwNDAwNDMzMjEzMzA1ODI4NjEzMCJdLCJlbWFpbCI6WyJhbmR5d2lyYW5hdGF3aWpheWFAZ21haWwuY29tIl19LCJzaWduX2luX3Byb3ZpZGVyIjoiZ29vZ2xlLmNvbSJ9fQ.gy-SzC2gt6pk9MLn8Y5BS0O8gnXybkzrGVdAnfPzF7YjBPBaLn5NhQ6kQnBVZIS2bEDxgjrj0q8g12rG0cEf6ESO5_mIm8bPixJyxz3gDWFs1q3DKX98S0pCVICJ4ay5tBIhaUjud1_7jUimGkikre-ZrPhUbJC7F82Fc9ydFEfoq8Dy4My8SG5dvxfW4NL2mKQ_N4p-MdmqzPYeFxjiGVLsyeItgh5XVK5YED5RYUtNLvVPl7YXEXv6Z8vpBCFi1h-ncZKtYua7vUmFU5yBUTwV5HRyRsiYjDOZ_wJnmViBbXIss6dOwOGCT4YyPYSVmegj0ghwBbQMUF2r24dDkQ");
//                System.out.println("Firebase Claims: " + firebaseClaims);
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//    }

    public static void main(String[] args) throws Exception {
        MainProxy app = new MainProxy();
        app.start();
    }
}
