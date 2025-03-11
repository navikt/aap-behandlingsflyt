package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent

data class VurderingerForSamordningUføre(
    val begrunnelse: String,
    val vurderingPerioder: List<SamordningUføreVurderingPeriode>,
)

data class SamordningUføreVurderingPeriode(
    val periode: Periode,
    val uføregradTilSamordning: Prosent
)
