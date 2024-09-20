package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.sykepenger

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.Prosent

data class SykepengerPeriode (
    val periode: Periode,
    val gradering: Prosent,
    val kronesum: Number?,
    val ytelse: String
)