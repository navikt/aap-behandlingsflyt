package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import java.time.LocalDate
import java.time.LocalDateTime

data class SamordningYtelse(
    val ytelseType: Ytelse,
    val ytelsePerioder: List<SamordningYtelsePeriode>,
    val kilde: String,
    val saksRef: String? = null,
)

data class SamordningYtelsePeriode(
    val periode: Periode,
    val gradering: Prosent?,
    val kronesum: Number? = null
)

data class SamordningVurdering(
    val ytelseType: Ytelse,
    val vurderingPerioder: List<SamordningVurderingPeriode>,
)

data class SamordningVurderingPeriode(
    val periode: Periode,
    val gradering: Prosent?,
    val kronesum: Number? = null,
    val manuell: Boolean?,
)

data class SamordningYtelseGrunnlag(
    val grunnlagId: Long,
    val ytelser: List<SamordningYtelse>,
)

data class SamordningVurderingGrunnlag(
    val vurderingerId: Long? = null,
    val begrunnelse: String?,
    val maksDatoEndelig: Boolean?,
    val fristNyRevurdering: LocalDate?,
    val vurderinger: List<SamordningVurdering>,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime? = null
)