package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.type.Periode

data class BarnetilleggPeriode(
    val periode: Periode,
    val personIdenter: Set<Ident>
)