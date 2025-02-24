package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent

data class SamordningYtelse(
    val ytelseType: Ytelse,
    val ytelsePerioder: List<SamordningYtelsePeriode>,
    val kilde: String,
    val saksRef: String?,
)

data class SamordningYtelsePeriode(
    val periode: Periode,
    val gradering: Prosent?,
    val kronesum: Number?
)

data class SamordningVurdering(
    val ytelseType: Ytelse,
    val vurderingPerioder: List<SamordningVurderingPeriode>,
)

data class SamordningVurderingPeriode(
    val periode: Periode,
    val gradering: Prosent?,
    val kronesum: Number?
)

data class SamordningYtelseVurderingGrunnlag(
    val ytelseGrunnlag: SamordningYtelseGrunnlag,
    val vurderingGrunnlag: SamordningVurderingGrunnlag
)

data class SamordningYtelseGrunnlag(
    val ytelseId: Long,
    val ytelser: List<SamordningYtelse>,
)

data class SamordningVurderingGrunnlag(
    val vurderingerId: Long?,
    val vurderinger: List<SamordningVurdering>,
)