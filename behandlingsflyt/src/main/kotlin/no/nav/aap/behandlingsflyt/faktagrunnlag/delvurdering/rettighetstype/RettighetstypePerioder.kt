package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode

data class RettighetstypePeriode(
    val periode: Periode,
    val rettighetstype: RettighetsType,
)

data class RettighetstypePerioder(
    val perioder: Set<RettighetstypePeriode>
) {
    fun somRettighetstypeTidslinje(): Tidslinje<RettighetsType> {
        return perioder.sortedBy { it.periode.fom }
            .somTidslinje { it.periode }
            .map { it.rettighetstype }
            .komprimer()
    }
}