package io.javafast.benchmark.vertx;

import io.javafast.benchmark.utils.HeapUsageMonitor;
import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;

public final class VertxServer {

    static class HttpServerVerticle extends AbstractVerticle {

        final String host = System.getProperty("server.host", "localhost");
        final int port = Integer.parseInt(System.getProperty("server.port", "8080"));

        @Override
        public void start(Promise<Void> startPromise) {
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
                .onSuccess(ignored -> {
                    IO.println("Vert.x server started on port " + port);
                    startPromise.succeed();
                })
                .onFailure(startPromise::fail);

        }

    }

    static void main() {
        final Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(HttpServerVerticle::new, new DeploymentOptions().setInstances(VertxOptions.DEFAULT_EVENT_LOOP_POOL_SIZE))
            .onSuccess(_ -> IO.println("Vert.x server deployed"))
            .onFailure(Throwable::printStackTrace);

        Runtime.getRuntime().addShutdownHook(new Thread(vertx::close));
        HeapUsageMonitor.start("servers/vertx/build/results/heap-usage.json");
    }
}
