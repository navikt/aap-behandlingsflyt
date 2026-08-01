package no.nav.aap.beregning

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent

data class UføregradDelperiode(
    val periode: Periode,
    val uføregrad: Prosent,
)