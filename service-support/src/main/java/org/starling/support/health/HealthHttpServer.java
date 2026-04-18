package org.starling.support.health;

import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.BooleanSupplier;

/**
 * Minimal HTTP health server for readiness and liveness probes.
 */
public final class HealthHttpServer implements AutoCloseable {

    private static final Logger log = LogManager.getLogger(HealthHttpServer.class);

    private final HttpServer server;
    private final ExecutorService executor;

    /**
     * Creates a new HealthHttpServer.
     * @param serviceName the service name value
     * @param port the port value
     * @param readiness the readiness value
     * @throws IOException if the operation fails
     */
    public HealthHttpServer(String serviceName, int port, BooleanSupplier readiness) throws IOException {
        Objects.requireNonNull(serviceName, "serviceName");
        Objects.requireNonNull(readiness, "readiness");

        server = HttpServer.create(new InetSocketAddress(port), 0);
        executor = Executors.newSingleThreadExecutor(new HealthThreadFactory(serviceName));
        server.setExecutor(executor);
        server.createContext("/health", exchange -> respond(exchange, 200, "ok"));
        server.createContext("/ready", exchange -> respond(exchange, readiness.getAsBoolean() ? 200 : 503,
                readiness.getAsBoolean() ? "ready" : "not_ready"));
    }

    /**
     * Starts.
     */
    public void start() {
        server.start();
        log.info("Health server listening on {}", server.getAddress());
    }

    /**
     * Closes.
     */
    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    /**
     * Responds.
     * @param exchange the exchange value
     * @param status the status value
     * @param body the body value
     * @throws IOException if the operation fails
     */
    private void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    /**
     * Creates the health server thread.
     */
    private static final class HealthThreadFactory implements ThreadFactory {

        private final String serviceName;

        /**
         * Creates a new HealthThreadFactory.
         * @param serviceName the service name value
         */
        private HealthThreadFactory(String serviceName) {
            this.serviceName = serviceName;
        }

        /**
         * News thread.
         * @param runnable the runnable value
         * @return the result of this operation
         */
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, serviceName + "-health");
            thread.setDaemon(true);
            return thread;
        }
    }
}
