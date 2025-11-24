package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import java.time.LocalDate
import java.time.LocalDateTime

data class SamordningYtelse(
    val ytelseType: Ytelse,
    val ytelsePerioder: Set<SamordningYtelsePeriode>,
    val kilde: String,
    val saksRef: String? = null,
)

data class SamordningYtelsePeriode(
    override val periode: Periode,
    val gradering: Prosent?,
    val kronesum: Number? = null
) : HarPeriode

data class SamordningVurdering(
    val ytelseType: Ytelse,
    val vurderingPerioder: Set<SamordningVurderingPeriode>,
)

data class SamordningVurderingPeriode(
    override val periode: Periode,
    val gradering: Prosent?,
    val kronesum: Number? = null,
    val manuell: Boolean?,
) : HarPeriode

data class SamordningYtelseGrunnlag(
    val grunnlagId: Long,
    val ytelser: Set<SamordningYtelse>,
)

data class SamordningVurderingGrunnlag(
    val vurderingerId: Long? = null,
    val begrunnelse: String?,
    val vurderinger: Set<SamordningVurdering>,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime? = null
)

interface HarPeriode {
    val periode: Periode
}