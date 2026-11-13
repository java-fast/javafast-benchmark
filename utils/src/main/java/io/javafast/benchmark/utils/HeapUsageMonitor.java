package io.javafast.benchmark.utils;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class HeapUsageMonitor {

    private static final long INTERVAL_MS = 250;
    private static final MemoryMXBean MEMORY_BEAN = ManagementFactory.getMemoryMXBean();
    private static final List<GarbageCollectorMXBean> GARBAGE_COLLECTORS = ManagementFactory.getGarbageCollectorMXBeans();
    private static final List<Sample> SAMPLES = new ArrayList<>();
    private static final long START_TIME = System.nanoTime();

    private static volatile boolean running;
    private static Thread collector;

    private HeapUsageMonitor() {
    }

    public static synchronized void start(String outputFile) {
        if (running) {
            throw new IllegalStateException("HeapUsageMonitor already started");
        }
        running = true;
        collector = Thread.ofPlatform()
            .name("heap-usage-monitor")
            .daemon(true)
            .start(HeapUsageMonitor::collect);
        Runtime.getRuntime().addShutdownHook(
            Thread.ofPlatform()
                .name("heap-usage-shutdown")
                .unstarted(() -> {
                    running = false;
                    if (collector != null) {
                        try {
                            collector.join();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    writeJson(outputFile);
                })
        );
    }

    private static void collect() {
        while (running) {
            final long elapsedMillis = (System.nanoTime() - START_TIME) / 1_000_000L;
            final long usedBytes = MEMORY_BEAN.getHeapMemoryUsage().getUsed();
            long gcCount = 0;
            long gcTime = 0;
            for (GarbageCollectorMXBean garbageCollector : GARBAGE_COLLECTORS) {
                gcCount += garbageCollector.getCollectionCount();
                gcTime += garbageCollector.getCollectionTime();
            }
            synchronized (SAMPLES) {
                SAMPLES.add(new Sample(elapsedMillis, usedBytes, gcCount, gcTime));
            }
            try {
                Thread.sleep(INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static void writeJson(String outputFile) {
        final List<Sample> samples;
        synchronized (SAMPLES) {
            samples = List.copyOf(SAMPLES);
        }
        final StringBuilder json = new StringBuilder(samples.size() * 32);
        json.append("{\n  \"heapUsage\": [");
        for (int i = 0; i < samples.size(); i++) {
            final Sample sample = samples.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("\n    [")
                .append(sample.elapsedMillis())
                .append(", ")
                .append(sample.usedBytes())
                .append(", ")
                .append(sample.gcCount())
                .append(", ")
                .append(sample.gcTime())
                .append(']');
        }
        if (!samples.isEmpty()) {
            json.append('\n').append("  ");
        }
        json.append("]\n}\n");
        try {
            final Path path = Path.of(outputFile);
            final Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to write heap usage data: " + e);
        }
    }

    private record Sample(long elapsedMillis, long usedBytes, long gcCount, long gcTime) {
    }
}
