package no.nav.syfo.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Histogram

const val METRICS_NS = "egenmeldtsmbackend"

val HTTP_HISTOGRAM: Histogram = Histogram.Builder()
    .labelNames("path")
    .name("requests_duration_seconds")
    .help("http requests durations for incoming requests in seconds")
    .register()

val EGENMELDT_SYKMELDING_REQ_V1_COUNTER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("egenmeldt_sykmelding_req_count")
        .help("Number of requests to sykmelding/egenmeldt")
        .register()

val EGENMELDT_SYKMELDING_COUNTER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("egenmeldt_sykmelding_count")
        .help("Number of egenmeldt sykmelding registered")
        .register()

val EGENMELDT_SYKMELDING_PERMUTED_COUNTER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("egenmeldt_sykmelding_permuted_count")
        .help("Number of egenmeldt sykmelding registered incl. permutations for each arbeidsgiver")
        .register()

val EGENMELDT_SYKMELDING_FAILED_COUNTER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("egenmeldt_sykmelding_failed_count")
        .help("Number of failed egenmeldt sykmelding requests")
        .register()