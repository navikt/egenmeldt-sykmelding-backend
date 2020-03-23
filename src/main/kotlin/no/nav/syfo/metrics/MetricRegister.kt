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

val EGENMELDT_SYKMELDING_REQ_COUNTER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("egenmeldt_sykmelding_req_count")
        .help("Number of egenmeldt sykmelding requests registered")
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

val EGENMELDT_SYKMELDING_ERROR_TOM_BEFORE_FOM_COUNTER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("egenmeldt_sykmelding_error_tom_before_fom_counter")
        .help("Number of TOM BEFORE FOM errors")
        .register()

val EGENMELDT_SYKMELDING_ALREADY_EXISTS_COUNTER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("egenmeldt_sykmelding_already_exists_counter")
        .help("Number of sykmelding already exists errors   ")
        .register()