package no.nav.aap.behandlingsflyt

import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.util.UUID

val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

fun PrometheusMeterRegistry.behandlingSattPåVentTeller(referanse: UUID) =
    this.counter(
        "behandlingsflyt_behandling_satt_pa_vent_total",
        listOf(Tag.of("referanse", referanse.toString()))
    )

fun PrometheusMeterRegistry.stegFullførtTeller(referanse: UUID) =
    this.counter(
        "behandlingsflyt_steg_fullfort_total",
        listOf(Tag.of("referanse", referanse.toString()))
    )