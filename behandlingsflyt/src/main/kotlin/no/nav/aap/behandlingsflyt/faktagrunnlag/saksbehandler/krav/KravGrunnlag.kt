package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav

import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid

data class KravGrunnlag(
    val vurderinger: Set<KravVurdering>,
) {
    fun gjeldendeVurderinger(): Set<KravVurdering> {
        return vurderinger
            .groupBy { it.referanse }
            .values
            .map { kravForReferanse -> kravForReferanse.maxBy { it.opprettet } }
            .toSet()
    }

    fun kravtidslinje(): Tidslinje<RelevantKrav> {
        return gjeldendeVurderinger()
            .filterIsInstance<RelevantKrav>()
            .sortedBy { it.muligRettFra }
            .somTidslinje { Periode(it.muligRettFra, Tid.MAKS) }
    }
}

