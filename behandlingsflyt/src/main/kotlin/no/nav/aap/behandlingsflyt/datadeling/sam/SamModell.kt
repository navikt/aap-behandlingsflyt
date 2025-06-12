package no.nav.aap.behandlingsflyt.datadeling.sam

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.samid.SamId
import java.time.LocalDate


data class SamordneVedtakRequest(
    val pid: String,
    val vedtakId: String,
    val sakId: Long,
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
    val samordningsmeldinger: List<SamordningsmeldingApi> = emptyList()
)

data class SamordningsmeldingApi(
    val samId: Long,
)