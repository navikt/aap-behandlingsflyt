package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent

data class SamordningUføreGrunnlag(
    val vurdering: SamordningUføreVurdering,
)

data class SamordningUføreVurdering(
    val begrunnelse: String,
    val vurderingPerioder: List<SamordningUføreVurderingPeriode>,
)

data class SamordningUføreVurderingPeriode(
    val periode: Periode,
    val uføregradTilSamordning: Prosent
)

data class SamordningUføreVurderingDto(
    val begrunnelse: String,
    val vurderingPerioder: List<SamordningUføreVurderingPeriodeDto>,
)

data class SamordningUføreVurderingPeriodeDto(
    val periode: Periode,
    val uføregradTilSamordning: Int
)
