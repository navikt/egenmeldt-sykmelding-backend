package no.nav.syfo.metrics

import io.prometheus.client.Histogram

const val METRICS_NS = "egenmeldtsmbackend"

val HTTP_HISTOGRAM: Histogram = Histogram.Builder()
    .labelNames("path")
    .name("requests_duration_seconds")
    .help("http requests durations for incoming requests in seconds")
    .register()