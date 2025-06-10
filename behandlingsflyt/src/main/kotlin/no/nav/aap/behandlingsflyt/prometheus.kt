package no.nav.aap.behandlingsflyt

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType

val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

fun PrometheusMeterRegistry.uhåndtertExceptionTeller(type: String): Counter =
    this.counter("behandlingsflyt_uhaandtert_exception_total", listOf(Tag.of("type", type)))

fun PrometheusMeterRegistry.dokumentHendelse(type: InnsendingType): Counter =
    this.counter(
        "behandlingsflyt_dokument_hendelse_total",
        listOf(Tag.of("hendelse", type.name))
    )