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

## Benchmarks

[HelloWorldSimulation](./benchmark-results/dashboard/index.html?simulation=HelloWorldSimulation)

Although a simple "Hello World" benchmark doesn't show the real world performance of a framework, it's a good starting point for comparing different frameworks. And the first results of JavaFast look promising, on the test machine achieving around **200K requests per second** under the load of **4K concurrent users**, beating most of the popular frameworks in the Java ecosystem.

### Quick Glance

In this benchmark, JavaFast ranks first across three key performance dimensions:

- **Throughput:** ~199K requests/sec, slightly ahead of Undertow and Vert.x. Throughput is relatively close across the top-performing frameworks.
- **Tail Latency:** Best P95 (22 ms) and P99 (24 ms) response times among the tested frameworks.
- **Memory Efficiency:** JavaFast's most significant advantage. It achieves the lowest average heap usage (155 MB) and only 41 ms of total GC time, compared to 232–245 ms for Undertow and Vert.x, and substantially more for the other frameworks.

While throughput differences between the leading frameworks are relatively small, JavaFast's memory efficiency stands out. Its minimal allocation overhead translates into significantly less garbage collection activity and a smaller memory footprint.

### How to Interpret Benchmark Results

Benchmark results should be considered a reference point rather than an absolute ranking of HTTP server frameworks.

A few things are worth keeping in mind when interpreting the results:

- **Throughput (RPS):** Represents the number of requests successfully processed per second. Higher throughput indicates greater request-processing capacity under the tested workload.
- **Response time percentiles:** Mean response time alone does not tell the whole story. P50 represents the median response time, while P95 and P99 show how the system behaves for slower requests. These percentiles are particularly useful for understanding latency consistency under load.
- **Concurrent users:** The benchmark uses up to 4,096 concurrent users to generate a sustained load. This does not necessarily mean that 4,096 requests are being processed simultaneously.
- **Error rate:** A high throughput number is meaningless if requests fail. All successful benchmark runs should be evaluated alongside their error counts.
- **Workload characteristics:** Hello World is intentionally simple. It primarily measures the HTTP server's request-processing overhead and does not represent applications involving database access, business logic, serialization, or external services.

The results are also influenced by hardware, operating system, JDK version, JVM configuration, network conditions, and benchmark methodology. Small differences in throughput may fall within normal run-to-run variation, so repeated runs are important before drawing conclusions.

JavaFast does not aim to be the fastest HTTP server in every scenario. Its primary goals are competitive throughput, near-zero allocation in the hot path, minimal GC overhead, and the simplicity of relying entirely on standard JDK APIs without external networking dependencies.

For this reason, throughput should be considered together with response-time percentiles, allocation rates, and GC activity when evaluating JavaFast.
