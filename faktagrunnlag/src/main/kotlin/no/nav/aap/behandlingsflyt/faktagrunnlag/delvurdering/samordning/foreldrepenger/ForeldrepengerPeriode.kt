package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.foreldrepenger

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.Prosent

data class ForeldrepengerPeriode (
    val periode: Periode,
    val gradering: Prosent,
    val ytelse: String
)