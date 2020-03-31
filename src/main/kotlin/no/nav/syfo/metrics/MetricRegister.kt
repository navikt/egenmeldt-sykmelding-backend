package no.nav.syfo.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Histogram

const val METRICS_NS = "egenmeldtsmbackend"

val HTTP_HISTOGRAM: Histogram = Histogram.Builder()
    .labelNames("path")
    .name("requests_duration_seconds")
    .help("http requests durations for incoming requests in seconds")
    .register()

val EGENMELDT_SYKMELDING_HTTP_REQ_COUNTER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("egenmeldt_sykmelding_http_req_count")
        .help("Number of http requests to sykmelding/egenmeldt")
        .register()

val EGENMELDT_SYKMELDING_COUNTER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("egenmeldt_sykmelding_count")
        .help("Total number of egenmeldt sykmelding registered")
        .register()

val EGENMELDT_SYKMELDING_FAILED_COUNTER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("egenmeldt_sykmelding_failed_count")
        .help("Number of failed egenmeldt sykmelding requests")
        .register()

val ERROR_HIT_COUNTER: Counter = Counter.Builder()
        .namespace(METRICS_NS)
        .name("egenmelding_error_counter")
        .labelNames("error_code")
        .help("Counter for de forskjellige errortypene")
        .register()
