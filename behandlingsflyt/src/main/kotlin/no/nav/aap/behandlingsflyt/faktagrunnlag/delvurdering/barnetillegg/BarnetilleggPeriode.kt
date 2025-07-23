package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.komponenter.type.Periode

data class BarnetilleggPeriode(
    val periode: Periode,
    val personIdenter: Set<BarnIdentifikator>
)