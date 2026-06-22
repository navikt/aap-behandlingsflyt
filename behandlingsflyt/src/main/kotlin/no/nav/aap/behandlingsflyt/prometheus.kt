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

fun PrometheusMeterRegistry.forutgåendeMedlemskapMedGapUtfall(resultat: Boolean): Counter =
    this.counter("behandlingsflyt_forutgaaende_medlemskap_med_gap_total", listOf(Tag.of("utfall", resultat.toString())))

fun PrometheusMeterRegistry.forutgåendeMedlemskapMedGapInntektsvurdering(resultat: Boolean): Counter =
    this.counter("behandlingsflyt_forutgaaende_medlemskap_med_gap_inntekt", listOf(Tag.of("inntekt", resultat.toString())))

fun PrometheusMeterRegistry.forutgåendeMedlemskapStandardGjennomslipp(resultat: Boolean): Counter =
    this.counter("behandlingsflyt_forutgaaende_medlemskap_automatisk_vurdert_standard", listOf(Tag.of("standardgjennomslipp", resultat.toString())))

fun PrometheusMeterRegistry.forutgåendeMedlemskapGapGjennomslipp(resultat: Boolean): Counter =
    this.counter("behandlingsflyt_forutgaaende_medlemskap_automatisk_vurdert_med_gap", listOf(Tag.of("gapgjennomslipp", resultat.toString())))

fun PrometheusMeterRegistry.forutgåendeMedlemskapNorskOgUtfallInntekt(resultat: Boolean): Counter =
    this.counter("behandlingsflyt_forutgaaende_medlemskap_norsk_utfall_inntekt", listOf(Tag.of("norskutfallinntek", resultat.toString())))

fun PrometheusMeterRegistry.forutgåendeMedlemskapNorskOgAvslag(resultat: Boolean): Counter =
    this.counter("behandlingsflyt_forutgaaende_medlemskap_norsk_avslag", listOf(Tag.of("norskavslag", resultat.toString())))

fun PrometheusMeterRegistry.lovvalgAutomatiskGjennomslipp(resultat: Boolean): Counter =
    this.counter("behandlingsflyt_lovvalg_automatisk_vurdert_total", listOf(Tag.of("lovvalggjennomslipp", resultat.toString())))

fun PrometheusMeterRegistry.lovvalgÅrsakTilManuellVurdering(årsak: String): Counter =
    this.counter("behandlingsflyt_lovvalg_aarsak_manuell_vurdering_total", listOf(Tag.of("aarsak", årsak)))

fun PrometheusMeterRegistry.lovvalgAutomatiskVurderingOverstyrt(): Counter =
    this.counter("behandlingsflyt_lovvalg_overstyrt_total")