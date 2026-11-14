package io.javafast.benchmark.helidon.nima;

import io.helidon.http.HeaderNames;
import io.helidon.webserver.WebServer;
import io.javafast.benchmark.utils.HeapUsageMonitor;

public final class HelidonNimaServer {

    static void main() {
        final String host = System.getProperty("server.host", "localhost");
        final int port = Integer.parseInt(System.getProperty("server.port", "8080"));

        final WebServer server = WebServer.builder()
            .host(host)
            .port(port)
            .routing(routing -> routing.get("/hello", (_, res) -> {
                res.headers().add(HeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8");
                res.send("Hello World");
            }))
            .build()
            .start();

        IO.println("Helidon Nima server started on port " + server.port());
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        HeapUsageMonitor.start("servers/helidon-nima/build/results/heap-usage.json");
    }
}
