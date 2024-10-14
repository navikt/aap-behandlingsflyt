package no.nav.aap.behandlingsflyt

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.util.UUID

val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

fun PrometheusMeterRegistry.behandlingSattPåVentTeller(): Counter =
    this.counter(
        "behandlingsflyt_behandling_satt_pa_vent_total",
        listOf()
    )

fun PrometheusMeterRegistry.stegFullførtTeller(stegNavn: String): Counter =
    this.counter(
        "behandlingsflyt_steg_fullfort_total",
        listOf(Tag.of("referanse", stegNavn))
    )


fun PrometheusMeterRegistry.uhåndtertExceptionTeller(type: String): Counter =
    this.counter("behandlingsflyt_uhaandtert_exception_total", listOf(Tag.of("type", type)))