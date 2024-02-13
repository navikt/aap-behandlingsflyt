package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg

import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident

class BarnetilleggPeriode(
    val periode: Periode,
    val personIdenter: List<Ident>
)