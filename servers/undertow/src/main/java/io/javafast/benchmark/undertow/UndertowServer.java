package io.javafast.benchmark.undertow;

import io.javafast.benchmark.utils.HeapUsageMonitor;
import io.undertow.Undertow;
import io.undertow.util.Headers;

public final class UndertowServer {

    static void main() {
        final String host = System.getProperty("server.host", "localhost");
        final int port = Integer.parseInt(System.getProperty("server.port", "8080"));

        final Undertow server = Undertow.builder()
            .addHttpListener(port, host)
            .setHandler(exchange -> {
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain; charset=utf-8");
                exchange.getResponseSender().send("Hello World");
            })
            .build();
        server.start();

        IO.println("Undertow server started on port " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        HeapUsageMonitor.start("servers/undertow/build/results/heap-usage.json");
    }
}
