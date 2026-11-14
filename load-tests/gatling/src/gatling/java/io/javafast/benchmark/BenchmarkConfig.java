package io.javafast.benchmark;

public final class BenchmarkConfig {

    public static final String BASE_URL = System.getProperty("benchmark.url", "http://127.0.0.1:8080");
    public static final int MAX_CONCURRENT_USERS = Integer.parseInt(System.getProperty("benchmark.max.concurrent.users", "4096"));

}
