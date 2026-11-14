package io.javafast.benchmark.javalin;

import io.javalin.Javalin;
import io.javafast.benchmark.utils.HeapUsageMonitor;

public final class JavalinServer {

    static void main() {
        final String host = System.getProperty("server.host", "localhost");
        final int port = Integer.parseInt(System.getProperty("server.port", "8080"));

        final Javalin server = Javalin.create(cfg -> {
            cfg.routes.get("/hello", ctx ->
                ctx.header("content-type", "text/plain; charset=utf-8").result("Hello World")
            );
        }).start(host, port);

        IO.println("Javalin server started on port " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        HeapUsageMonitor.start("servers/javalin/build/results/heap-usage.json");
    }
}
