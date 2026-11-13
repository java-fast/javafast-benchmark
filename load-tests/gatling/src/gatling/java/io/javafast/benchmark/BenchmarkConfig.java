package io.javafast.benchmark;

public final class BenchmarkConfig {

    private BenchmarkConfig() {
    }

    public static final String BASE_URL = System.getProperty("benchmark.url", "http://127.0.0.1:8080");
}
