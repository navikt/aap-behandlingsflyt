package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent

data class SamordningPeriode (
    val periode: Periode,
    val gradering: Prosent
)