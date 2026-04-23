package com.westminster.smartcampus;

import com.westminster.smartcampus.config.AppConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

import java.net.URI;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    public static final String BASE_URI = "http://0.0.0.0:8080/";

    public static void main(String[] args) throws InterruptedException {
        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(
                URI.create(BASE_URI),
                new AppConfig()
        );

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown signal received. Stopping server...");
            server.shutdownNow();
            LOGGER.info("Server stopped.");
        }));

        LOGGER.info("============================================");
        LOGGER.info("  Smart Campus API started successfully.");
        LOGGER.info("  Base URL : http://localhost:8080/api/v1");
        LOGGER.info("  Press Ctrl+C to stop the server.");
        LOGGER.info("============================================");

        Thread.currentThread().join();
    }
}
