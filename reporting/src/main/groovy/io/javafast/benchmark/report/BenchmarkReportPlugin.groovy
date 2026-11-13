package io.javafast.benchmark.report

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Registers extraction and multi-server aggregation tasks for Gatling HTML reports
 */
class BenchmarkReportPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def serverId = project.providers.gradleProperty('server').orElse('javafast')
        def serverName = project.providers.gradleProperty('serverName').orElse(serverId)
        def simulation = project.providers.gradleProperty('simulation').orElse('HelloWorldSimulation')
        def executionModel = project.providers.gradleProperty('executionModel').orElse('Non-blocking')
        def threads = project.providers.gradleProperty('threads').orElse('1')
        def requestExecution = project.providers.gradleProperty('requestExecution').orElse('Virtual threads')
        def protocol = project.providers.gradleProperty('protocol').orElse('HTTP/1.1')
        def reportDir = project.providers.gradleProperty('reportDir')
          .map { String path -> project.layout.projectDirectory.dir(path) }
          .orElse(project.layout.projectDirectory.dir('build/reports/gatling/latest'))

        def resultsDir = project.layout.dir(project.provider {
            project.file("benchmark-results/data/${simulation.get()}")
        })
        def extractedFile = project.layout.file(project.provider {
            project.file("benchmark-results/data/${simulation.get()}/${serverId.get()}.json")
        })
        def heapUsageFile = project.layout.file(project.provider {
            project.file("servers/${serverId.get()}/build/results/heap-usage.json")
        })
        def combinedFile = project.layout.file(project.provider {
            project.file("benchmark-results/dashboard/data/${simulation.get()}.json")
        })

        project.tasks.register('extractBenchmarkResults', ExtractBenchmarkResultsTask) { task ->
            task.group = 'benchmark reporting'
            task.description = 'Extracts normalized metrics and throughput from a Gatling HTML report.'
            task.serverId.set(serverId)
            task.serverName.set(serverName)
            task.simulation.set(simulation)
            task.executionModel.set(executionModel)
            task.threads.set(threads)
            task.requestExecution.set(requestExecution)
            task.protocol.set(protocol)
            task.reportDirectory.set(reportDir)
            task.heapUsageFile.set(heapUsageFile)
            task.outputFile.set(extractedFile)
        }

        project.tasks.register('mergeBenchmarkResults', MergeBenchmarkResultsTask) { task ->
            task.group = 'benchmark reporting'
            task.description = 'Merges extracted server results into one simulation JSON file.'
            task.dependsOn('extractBenchmarkResults')
            task.simulation.set(simulation)
            task.resultsDirectory.set(resultsDir)
            task.outputFile.set(combinedFile)
        }

        project.tasks.register('benchmarkReport') { task ->
            task.group = 'benchmark reporting'
            task.description = 'Extracts the current Gatling report and merges it into the simulation results.'
            task.dependsOn('mergeBenchmarkResults')
        }
    }
}

abstract class ExtractBenchmarkResultsTask extends DefaultTask {
    @Input abstract Property<String> getServerId()
    @Input abstract Property<String> getServerName()
    @Input abstract Property<String> getSimulation()
    @Input abstract Property<String> getExecutionModel()
    @Input abstract Property<String> getThreads()
    @Input abstract Property<String> getRequestExecution()
    @Input abstract Property<String> getProtocol()

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getReportDirectory()

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getHeapUsageFile()

    @OutputFile abstract RegularFileProperty getOutputFile()

    @TaskAction
    void extractReport() {
        File report = reportDirectory.get().asFile
        BenchmarkResult result = new GatlingReportExtractor().extract(
            report,
            serverId.get(),
            serverName.get(),
            simulation.get(),
            executionModel.get(),
            threads.get(),
            requestExecution.get(),
            protocol.get()
        )

        File heapFile = heapUsageFile.get().asFile
        if (!heapFile.isFile()) {
            throw new IllegalStateException("Missing heap usage JSON: ${heapFile}")
        }
        Map<String, Object> heapUsage = (Map<String, Object>) new JsonSlurper().parse(heapFile, 'UTF-8')
        result.heapUsage = (List<List<Number>>) (heapUsage.heapUsage ?: [])

        File output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.setText(JsonOutput.prettyPrint(JsonOutput.toJson(result.toMap())) + System.lineSeparator(), 'UTF-8')

        logger.lifecycle("Extracted ${result.throughput.size()} throughput samples for ${result.serverName} -> ${output}")
        logger.lifecycle("Extracted ${result.concurrency.size()} concurrency samples for ${result.serverName} -> ${output}")
        logger.lifecycle("Requests: ${result.metrics.total}, mean RPS: ${result.metrics.meanRps}, p99: ${result.metrics.p99} ms")
    }
}

abstract class MergeBenchmarkResultsTask extends DefaultTask {
    @Input abstract Property<String> getSimulation()

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getResultsDirectory()

    @OutputFile abstract RegularFileProperty getOutputFile()

    @TaskAction
    void mergeResults() {
        File directory = resultsDirectory.get().asFile
        List<File> files = (directory.listFiles() ?: [] as File[])
            .findAll { File file -> file.isFile() && file.name.endsWith('.json') }
            .sort { File a, File b -> a.name <=> b.name }

        if (files.isEmpty()) {
            throw new IllegalStateException("No extracted benchmark JSON files found in ${directory}")
        }

        Map<String, Object> servers = new LinkedHashMap<>()
        files.each { File file ->
            Map<String, Object> extracted = (Map<String, Object>) new JsonSlurper().parse(file, 'UTF-8')
            String extractedSimulation = extracted.simulation?.toString()
            if (extractedSimulation != simulation.get()) {
                throw new IllegalStateException("${file.name} belongs to simulation '${extractedSimulation}', expected '${simulation.get()}'")
            }
            Map server = (Map) extracted.server
            String id = server?.id?.toString()
            if (!id) throw new IllegalStateException("Missing server.id in ${file}")
            if (servers.containsKey(id)) throw new IllegalStateException("Duplicate server id '${id}' in ${directory}")

            servers.put(id, [
                name        : server.name,
                environment : extracted.environment,
                gatling     : extracted.gatling,
                run         : extracted.run,
                requestName : extracted.requestName,
                metrics     : extracted.metrics,
                requests    : extracted.requests,
                throughput  : extracted.throughput,
                concurrency : extracted.concurrency,
                heapUsage   : extracted.heapUsage
            ])
        }

        Map<String, Object> combined = [
            schemaVersion: BenchmarkResult.SCHEMA_VERSION,
            generatedAt  : new Date().format("yyyy-MM-dd'T'HH:mm:ssXXX", TimeZone.getTimeZone('UTC')),
            simulation   : simulation.get(),
            servers      : servers
        ]

        File output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.setText(JsonOutput.prettyPrint(JsonOutput.toJson(combined)) + System.lineSeparator(), 'UTF-8')
        logger.lifecycle("Merged ${servers.size()} server result(s) -> ${output}")
    }
}
