package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav

import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import kotlin.collections.filterIsInstance

data class KravGrunnlag(
    val vurderinger: Set<KravVurdering>,
) {
    fun gjeldendeVurderinger(): Set<KravVurdering> {
        return vurderinger.gjeldendeVurderinger()
    }

    /** Alle gjeldende relevante krav, både fra søknad og fra migrering. */
    fun kravtidslinje(): Tidslinje<RelevantKrav> {
        return vurderinger.kravtidslinje()
    }

    /** Alle gjeldende relevante krav, både fra søknad og fra migrering. */
    fun gjeldendeRelevanteKravInkludertMigrerteKrav(): Set<RelevantKrav> {
        return this.gjeldendeVurderinger().filterIsInstance<RelevantKrav>().toSet()
    }

    fun gjeldendeSøknadKrav(): Set<RelevantSøknadKrav> {
        return this.gjeldendeVurderinger().filterIsInstance<RelevantSøknadKrav>().toSet()
    }

    fun gjeldendeMigrertKrav(): RelevantMigrertKrav? {
        return this.gjeldendeVurderinger().filterIsInstance<RelevantMigrertKrav>().singleOrNull()
    }
}

fun Set<KravVurdering>.gjeldendeVurderinger(): Set<KravVurdering> {
    return this.groupBy { it.referanse }
        .values
        .map { kravForReferanse -> kravForReferanse.maxBy { it.opprettet } }
        .toSet()
}

fun Set<KravVurdering>.kravtidslinje(): Tidslinje<RelevantKrav> {
    return this.gjeldendeVurderinger()
        .filterIsInstance<RelevantKrav>()
        .sortedBy { it.muligRettFra }
        .somTidslinje { Periode(it.muligRettFra, Tid.MAKS) }
}
