package io.javafast.benchmark.javafast;

import io.javafast.JavaFast;
import io.javafast.benchmark.utils.HeapUsageMonitor;

/**
 * Write tests for real-world cases:
 * 1. JSON serialization/deserialization
 * 2. Big request and response body
 * 3. Keep-alive and many concurrent connections
 * 4. Routing and middleware
 * 5. JSON API + database latency
 * 6. Backpressure and overload behavior
 */
public final class JavaFastServer {

    static void main() {
        final String host = System.getProperty("server.host", "localhost");
        final int port = Integer.parseInt(System.getProperty("server.port", "8080"));
        final JavaFast app = JavaFast.create(cfg -> {
            cfg.routes.get("/hello", ctx -> {
                ctx.header("content-type", "text/plain; charset=utf-8")
                    .result("Hello World");
            });
        }).start(host, port);

        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
        HeapUsageMonitor.start("servers/javafast/build/results/heap-usage.json");
    }

}
