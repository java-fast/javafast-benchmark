package io.javafast.benchmark.report

import groovy.transform.CompileStatic

/**
 * Normalized, server-independent result extracted from a Gatling HTML report.
 * Throughput points use elapsed seconds from the first Gatling sample, not epoch time.
 */
@CompileStatic
class BenchmarkResult {
    static final int SCHEMA_VERSION = 1

    String simulation
    String serverId
    String serverName
    String gatlingVersion
    String runDate
    String duration
    String source = 'gatling-html-report'
    String requestName

    /** execution model */
    Map<String, String> executionModel = [:]

    /** Global Gatling statistics (all requests). */
    Map<String, Object> metrics = [:]

    /** Request-specific statistics, keyed by Gatling request id. */
    Map<String, Map<String, Object>> requests = [:]

    /** [elapsedSeconds, requestsPerSecond] pairs. */
    List<List<Number>> throughput = []

    /** [elapsedSeconds, activeUsers] pairs. */
    List<List<Number>> concurrency = []

    /** [elapsedSeconds, heapUsageBytes] pairs. */
    List<List<Number>> heapUsage = []

    Map<String, Object> toMap() {
        [
            schemaVersion: SCHEMA_VERSION,
            generatedAt  : new Date().format("yyyy-MM-dd'T'HH:mm:ssXXX", TimeZone.getTimeZone('UTC')),
            simulation   : simulation,
            server       : [id: serverId, name: serverName],
            environment  : [executionModel: executionModel],
            gatling      : [version: gatlingVersion],
            run          : [date: runDate, duration: duration],
            requestName  : requestName,
            metrics      : metrics,
            requests     : requests,
            throughput   : throughput,
            concurrency  : concurrency,
            heapUsage    : heapUsage,
            source       : source
        ] as Map<String, Object>
    }
}
