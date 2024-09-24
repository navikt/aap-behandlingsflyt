package no.nav.aap.behandlingsflyt

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

fun PrometheusMeterRegistry.behandlingStoppetOppTeller() =
    this.counter(
        "behandlingsflyt_behandling_stoppet_opp_total",
        listOf()
    )