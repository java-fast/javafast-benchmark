package io.javafast.benchmark.vertx;

import io.javafast.benchmark.utils.HeapUsageMonitor;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;

public final class VertxServer {

    private VertxServer() {
    }

    static void main() {
        final String host = System.getProperty("server.host", "localhost");
        final int port = Integer.parseInt(System.getProperty("server.port", "8080"));

        final Vertx vertx = Vertx.vertx();
        final HttpServer server = vertx.createHttpServer(new HttpServerOptions()
                .setHost(host)
                .setPort(port)
                .setTcpKeepAlive(true));

        server.requestHandler(request -> {
            if ("GET".equals(request.method().name()) && "/hello".equals(request.path())) {
                request.response()
                    .setStatusCode(200)
                    .putHeader("content-type", "text/plain; charset=utf-8")
                    .end("Hello World");
            } else {
                request.response().setStatusCode(404).end();
            }
        });

        server.listen()
                .onSuccess(ignored -> IO.println("Vert.x server listening on port " + port))
                .onFailure(error -> {
                    error.printStackTrace();
                    System.exit(1);
                });

        Runtime.getRuntime().addShutdownHook(new Thread(vertx::close));

        HeapUsageMonitor.start("servers/vertx/build/results/heap-usage.json");
    }
}
