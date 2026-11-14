package io.javafast.benchmark;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class HelloWorldSimulation extends Simulation {

    private final HttpProtocolBuilder httpProtocol = http
        .baseUrl(BenchmarkConfig.BASE_URL)
        .acceptHeader("text/plain")
        .userAgentHeader("JavaFast-Benchmark");

    private final ScenarioBuilder helloWorld = scenario("Hello World")
        .during(60)
        .on(
            exec(
                http("GET /hello")
                    .get("/hello")
                    .check(status().is(200))
                    .check(bodyString().is("Hello World"))
            )
        );

    {
        setUp(
            helloWorld.injectClosed(
                rampConcurrentUsers(1).to(5_000).during(60),
                constantConcurrentUsers(5_000).during(30)
            )
        ).protocols(httpProtocol)
            .assertions(global().failedRequests().count().is(0L));
    }
}
