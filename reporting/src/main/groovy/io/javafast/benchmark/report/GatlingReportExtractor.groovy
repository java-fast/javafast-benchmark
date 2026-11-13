package io.javafast.benchmark.report

import groovy.json.JsonSlurper
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

import java.math.RoundingMode
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Extracts normalized benchmark data from a Gatling 3.15.x HTML report
 */
class GatlingReportExtractor {
    private static final Map<Integer, String> METRIC_COLUMNS = [
        2 : 'total',
        3 : 'ok',
        4 : 'ko',
        5 : 'koPercent',
        6 : 'meanRps',
        7 : 'min',
        8 : 'p50',
        9 : 'p75',
        10: 'p95',
        11: 'p99',
        12: 'max',
        13: 'mean',
        14: 'stdDev'
    ].asImmutable()

    BenchmarkResult extract(File reportDirectory, String serverId, String serverName, String simulationOverride, String ioModel, String ioThreads, String requestExecution, String protocol) {
        if (!reportDirectory.isDirectory()) {
            throw new IllegalArgumentException("Gatling report directory does not exist: ${reportDirectory}")
        }

        File indexFile = new File(reportDirectory, 'index.html')
        if (!indexFile.isFile()) {
            throw new IllegalArgumentException("Missing Gatling index.html: ${indexFile}")
        }

        Document document = Jsoup.parse(indexFile, 'UTF-8')
        BenchmarkResult result = new BenchmarkResult()
        result.serverId = requireText(serverId, 'serverId')
        result.serverName = serverName?.trim() ? serverName.trim() : serverId
        result.executionModel = [ "I/O model": ioModel, "I/O threads": ioThreads, "Request execution": requestExecution, "Protocol": protocol ]
        result.simulation = simulationOverride?.trim() ? simulationOverride.trim() : textOf(document, '.onglet')
        result.gatlingVersion = valueForLabel(document, 'Version:')
        result.runDate = valueForLabel(document, 'Date:')
        result.duration = valueForLabel(document, 'Duration:')

        Element globalRow = document.selectFirst('#container_statistics_head tbody tr#ROOT')
        if (globalRow == null) {
            throw new IllegalStateException('Could not find global statistics row #ROOT in index.html')
        }
        result.metrics = metricsFromRow(globalRow)

        Element requestTable = document.selectFirst('#container_statistics_body tbody')
        if (requestTable == null) {
            throw new IllegalStateException('Could not find request statistics table in index.html')
        }

        requestTable.select('tr').each { Element row ->
            String requestId = row.id()?.trim()
            String requestName = row.selectFirst('td.col-1 .ellipsed-name')?.text()?.trim()
            if (!requestId || !requestName) {
                return
            }
            Map<String, Object> request = metricsFromRow(row)
            request.put('name', requestName)
            result.requests.put(requestId, request)
        }

        if (result.requests.isEmpty()) {
            throw new IllegalStateException('No request-specific statistics rows found in index.html')
        }
        result.requestName = result.requests.size() == 1
            ? result.requests.values().iterator().next().get('name')?.toString()
            : 'All Requests'

        def text = indexFile.getText('UTF-8')
        result.throughput = extractThroughput(text)
        result.concurrency = extractConcurrency(text)
        validateResult(result)
        return result
    }

    private static Map<String, Object> metricsFromRow(Element row) {
        Map<String, Object> metrics = new LinkedHashMap<>()
        METRIC_COLUMNS.each { Integer column, String metric ->
            Element cell = row.selectFirst("td.col-${column}")
            if (cell != null) {
                String value = cell.text()?.trim()
                if (value && value != '-') {
                    metrics.put(metric, parseNumber(value))
                }
            }
        }
        return metrics
    }

    private static List<List<Number>> extractTimeSeries(String html, String chartId, String seriesName, String valueName) {
        int chartIndex = html.indexOf("renderTo: '${chartId}'")
        if (chartIndex < 0) {
            chartIndex = html.indexOf("renderTo: \"${chartId}\"")
        }
        if (chartIndex < 0) {
            throw new IllegalStateException("Could not locate Gatling's ${chartId} chart")
        }

        Matcher seriesMatcher = Pattern.compile('\\bseries\\s*:\\s*\\[').matcher(html)
        seriesMatcher.region(chartIndex, html.length())
        if (!seriesMatcher.find()) {
            throw new IllegalStateException("Could not locate ${valueName} chart series")
        }

        // Locate the requested series by name.
        int seriesStart = seriesMatcher.end()
        String seriesText = readBalancedArray(html, seriesStart - 1)
        Matcher nameMatcher = Pattern.compile(
          "name\\s*:\\s*['\"]${Pattern.quote(seriesName)}['\"]"
        ).matcher(seriesText)

        if (!nameMatcher.find()) {
            throw new IllegalStateException("Could not locate '${seriesName}' series in ${chartId}")
        }

        Matcher dataMatcher = Pattern.compile('\\bdata\\s*:\\s*\\[').matcher(seriesText)
        dataMatcher.region(nameMatcher.end(), seriesText.length())
        if (!dataMatcher.find()) {
            throw new IllegalStateException("Could not locate ${valueName} chart data array")
        }

        int arrayStart = seriesText.indexOf('[', dataMatcher.start())
        String arrayText = readBalancedArray(seriesText, arrayStart)
        Object parsed = new JsonSlurper().parseText(arrayText)

        if (!(parsed instanceof List)) {
            throw new IllegalStateException("Gatling ${valueName} data is not a JSON array")
        }

        List<List<Number>> rawPoints = []
        ((List) parsed).eachWithIndex { Object point, int index ->
            if (!(point instanceof List) || ((List) point).size() < 2) {
                throw new IllegalStateException("Invalid ${valueName} point at index ${index}: ${point}")
            }

            Number timestamp = asNumber(((List) point).get(0), "${valueName} timestamp ${index}")
            Number value = asNumber(((List) point).get(1), "${valueName} value ${index}")
            rawPoints.add([timestamp, value])
        }

        if (rawPoints.isEmpty()) {
            throw new IllegalStateException("Gatling ${valueName} chart contains no data points")
        }

        long firstTimestamp = rawPoints.first().get(0).longValue()
        long previousTimestamp = firstTimestamp

        List<List<Number>> elapsedPoints = []
        rawPoints.eachWithIndex { List<Number> point, int index ->
            long timestamp = point.get(0).longValue()

            if (timestamp < previousTimestamp) {
                throw new IllegalStateException("${valueName.capitalize()} timestamps are not monotonically increasing")
            }

            previousTimestamp = timestamp
            long elapsedSeconds = (long) ((timestamp - firstTimestamp) / 1000L)
            elapsedPoints.add([elapsedSeconds, point.get(1)])
        }

        return elapsedPoints
    }

    private static List<List<Number>> extractThroughput(String html) {
        return extractTimeSeries(
          html,
          'RequestsContainerId',
          'All',
          'throughput'
        )
    }

    private static List<List<Number>> extractConcurrency(String html) {
        return extractTimeSeries(
          html,
          'MaxConcurrentUsersContainerId',
          'All users',
          'concurrency'
        )
    }

    /** Reads a balanced JavaScript array; Gatling emits numeric JSON-compatible chart data. */
    private static String readBalancedArray(String source, int start) {
        if (start < 0 || source.charAt(start) != '[') {
            throw new IllegalStateException('Expected an array at the throughput data location')
        }
        int depth = 0
        for (int i = start; i < source.length(); i++) {
            char current = source.charAt(i)
            if (current == '[') depth++
            if (current == ']') {
                depth--
                if (depth == 0) return source.substring(start, i + 1)
            }
        }
        throw new IllegalStateException('Unterminated throughput data array in index.html')
    }

    private static String valueForLabel(Document document, String label) {
        for (Element item : document.select('.simulation-information-item')) {
            Element labelElement = item.selectFirst('.simulation-information-label')
            if (labelElement != null && labelElement.text().trim() == label) {
                List<Element> spans = item.select('span')
                if (spans.size() > 1) return spans.last().text().trim()
            }
        }
        return null
    }

    private static String textOf(Document document, String selector) {
        return document.selectFirst(selector)?.text()?.trim()
    }

    private static Object parseNumber(String value) {
        BigDecimal number = new BigDecimal(value.replace(',', '').trim())
        return number.stripTrailingZeros().scale() <= 0 ? number.longValueExact() : number
    }

    private static Number asNumber(Object value, String description) {
        if (!(value instanceof Number)) {
            throw new IllegalStateException("Expected numeric ${description}, got ${value}")
        }
        return (Number) value
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Gradle property '${name}' must not be empty")
        }
        return value.trim()
    }

    private static void validateResult(BenchmarkResult result) {
        if (result.throughput.isEmpty()) throw new IllegalStateException('Missing throughput time series')
        if (result.concurrency.isEmpty()) throw new IllegalStateException('Missing concurrency time series')
        Object total = result.metrics.get('total')
        Object ok = result.metrics.get('ok')
        Object ko = result.metrics.get('ko')
        if (total == null || ok == null || ko == null) {
            throw new IllegalStateException('Global statistics are missing total/ok/ko counts')
        }
        if (((Number) total).longValue() != ((Number) ok).longValue() + ((Number) ko).longValue()) {
            throw new IllegalStateException("Inconsistent global request counts: total=${total}, ok=${ok}, ko=${ko}")
        }
    }
}
