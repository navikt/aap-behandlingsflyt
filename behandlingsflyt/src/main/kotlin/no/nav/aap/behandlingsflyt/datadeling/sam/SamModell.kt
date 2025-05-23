package no.nav.aap.behandlingsflyt.datadeling.sam

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.samid.SamId
import java.time.LocalDate


data class SamordneVedtakRequest(
    val pid: String,
    val vedtakId: String,
    val sakId: String,
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Europe/Oslo")
    val virkFom: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Europe/Oslo")
    val virkTom: LocalDate?,
    val fagomrade: String,
    val ytelseType: String,
    val etterbetaling: Boolean,
    val regelverkType: String? = null,
    val utvidetFrist: Boolean? = null,
)

data class SamordneVedtakRespons(val ventPaaSvar: Boolean)

data class HentSamIdResponse(
    val samordningVedtakId: Long,
    val fagsystem: String,
    val saksId: Long,
    val saksKode: String,
    val vedtakId: Long,
    val vedtakstatusKode: String?,
    val etterbetaling: Boolean,
    val utvidetSamordningsfrist: Boolean,
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Europe/Oslo")
    val virkningFom: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Europe/Oslo")
    val virkningTom: LocalDate?,
    val versjon: Long,
    val samordningsmeldinger: List<SamordningsmeldingApi> = emptyList()
)

data class SamordningsmeldingApi(
    val samId: Long,
    val meldingstatusKode: String,
    val tpNr: String,
    val tpNavn: String,
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Europe/Oslo")
    val sendtDato: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Europe/Oslo")
    val svartDato: LocalDate?,
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Europe/Oslo")
    val purretDato: LocalDate?,
    val refusjonskrav: Boolean,
    val versjon: Long
)