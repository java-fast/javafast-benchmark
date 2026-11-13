# JavaFast Gatling benchmark report tasks

These Groovy sources are intended to live in `reporting` module so the reporting workflow stays independent.
The extractor targets the Gatling 3.15.1 HTML report structure supplied with this project (not the internal binary `simulation.log` format).

## 1. Installation

Add the local plugin into `../settings.gradle`:

```groovy
pluginManagement {
    includeBuild('reporting')
}
```

Then, apply the plugin from the root `../build.gradle`:

```groovy
plugins {
    id 'io.javafast.benchmark.report'
}
```

## 2. Run it

Point `reportDir` at the *specific Gatling run directory* containing `index.html`:

```bash
./gradlew benchmarkReport \
  -Pserver=javafast \
  -PserverName=JavaFast \
  -Psimulation=HelloWorldSimulation \
  -PexecutionModel="Non-blocking" \
  -Pthreads=10 \
  -PrequestExecution="Virtual threads" \
  -Pprotocol="HTTP/1.1" \
  -PreportDir=load-tests/gatling/build/reports/gatling/helloworldsimulation-20261110232320927
```

The tasks are also available separately:

```bash
./gradlew extractBenchmarkResults -Pserver=javafast -Psimulation=HelloWorldSimulation -PreportDir=...
./gradlew mergeBenchmarkResults   -Pserver=javafast -Psimulation=HelloWorldSimulation -PreportDir=...
```

`mergeBenchmarkResults` depends on extraction, so it always includes the current run before merging all previously extracted server files.

## 3. Output

```text
benchmark-results/
├── data/
│   └── HelloWorldSimulation/
│       ├── javafast.json
│       └── vertx.json
└── dashboard/
    └── data/
        └── HelloWorldSimulation.json
```

The combined file has a `servers` object keyed by stable server id. Each server contains global metrics, request-specific metrics and throughput points in `[elapsedSeconds, requestsPerSecond]` format. The source report and its original epoch timestamps are not modified.

## Notes

- The parser reads `index.html`; it does not need `simulation.log`.
- Jsoup is used for HTML tables and metadata. The throughput array is read as a balanced numeric JavaScript array and decoded with Groovy's `JsonSlurper`.
- The global metrics come from the `ROOT` row. Per-request metrics come from `#container_statistics_body`.
- Gatling's HTML is not a versioned public data API. Keep a fixture report in tests and validate the parser when upgrading Gatling.
- `min` can legitimately be `0` in Gatling's report; the extractor preserves it.
