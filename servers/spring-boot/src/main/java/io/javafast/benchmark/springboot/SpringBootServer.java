package io.javafast.benchmark.springboot;

import io.javafast.benchmark.utils.HeapUsageMonitor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class SpringBootServer {

    static void main(String[] args) {
        SpringApplication.run(SpringBootServer.class, args);
        HeapUsageMonitor.start("servers/spring-boot/build/results/heap-usage.json");
    }

    @RestController
    static class HelloWorldController {
        @GetMapping(value = "/hello", produces = "text/plain")
        String helloWorld() {
            return "Hello World";
        }
    }
}
