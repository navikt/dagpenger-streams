package no.nav.dagpenger.streams

import io.prometheus.client.Summary

val processTimeLatency: Summary = Summary.build()
    .name("process_time_seconds")
    .quantile(0.5, 0.05) // Add 50th percentile (= median) with 5% tolerated error
    .quantile(0.9, 0.01) // Add 90th percentile with 1% tolerated error
    .quantile(0.99, 0.001) // Add 99th percentile with 0.1% tolerated error
    .help("Process time for a single packet")
    .register()
