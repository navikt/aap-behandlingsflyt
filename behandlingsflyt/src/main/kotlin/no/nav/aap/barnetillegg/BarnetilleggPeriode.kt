package no.nav.aap.barnetillegg

import no.nav.aap.komponenter.type.Periode

data class BarnetilleggPeriode(
    val periode: Periode,
    val personIdenter: Set<BarnIdentifikator>
)