# JavaFast Benchmark

A Gradle multi-project for comparing JavaFast and some other popular frameworks with a shared Gatling HTTP scenario.

## Modules

- `servers:javafast` — JavaFast server exposing `GET /hello`
- `servers:vertx` — Vert.x HTTP server exposing `GET /hello`.
- `load-tests:gatling` — shared Gatling Java DSL simulation and HTML reports.
- `reporting` — Gradle plugin for generating benchmark reports.

## Requirements

- JDK 25+
- Gradle 9.0.0+ (or a compatible Gradle version)

## Run Vert.x

Start the server in one terminal:

```bash
./gradlew :servers:vertx:run
```

Run the Gatling scenario in another terminal:

```bash
./gradlew :load-tests:gatling:gatlingRun
  --simulation io.javafast.benchmark.HelloWorldSimulation
  -Dbenchmark.url=http://127.0.0.1:8080
```

## Run JavaFast

Start the server in one terminal:

```bash
./gradlew :servers:javafast:run
```

## Gatling reports

Gatling writes an HTML report under:

`load-tests/gatling/build/reports/gatling/`

Each simulation run produces a report directory. Avoid running multiple Gatling tasks concurrently into the same output directory.

## Benchmark Results

Although a simple "Hello World" benchmark doesn't show the real world performance of a framework, it's a good starting point for comparing different frameworks. And the first results for JavaFast look promising, achieving over **200K requests per second** under the load of **5K concurrent users**, beating most of the popular frameworks in the Java ecosystem.

You can find the benchmark results below:

[HelloWorldSimulation](./benchmark-results/dashboard/index.html?simulation=HelloWorldSimulation)
