package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent

data class SamordningYtelse(
    val ytelseType: String,
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
    val ytelseType: String,
    val vurderingPerioder: List<SamordningVurderingPeriode>,
)

data class SamordningVurderingPeriode(
    val periode: Periode,
    val gradering: Prosent?,
    val kronesum: Number?
)

data class SamordningYtelseVurderingGrunnlag(
    val vurderingerId: Long?,
    val ytelserId: Long,
    val ytelser: List<SamordningYtelse>,
    val vurderinger: List<SamordningVurdering>?,
)