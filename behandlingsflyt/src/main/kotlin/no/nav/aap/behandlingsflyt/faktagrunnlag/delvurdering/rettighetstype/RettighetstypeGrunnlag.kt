package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode

data class RettighetstypeMedKvote(
    val periode: Periode,
    val rettighetstype: RettighetsType,
    val avslagsårsaker: Set<Avslagsårsak>,
    val brukerAvKvoter: Set<Kvote>
) {
    fun erAvslåttPåKvote() = avslagsårsaker.isNotEmpty()
}

data class RettighetstypeGrunnlag(
    val perioder: Set<RettighetstypeMedKvote>
) {
    fun somRettighetstypeTidslinje(): Tidslinje<RettighetsType> {
        return perioder.sortedBy { it.periode.fom }
            .somTidslinje { it.periode }
            .map { it.rettighetstype }
            .komprimer()
    }

    fun somRettighetstypeTidslinjeJustertForKvote(): Tidslinje<RettighetsType> {
        return perioder.sortedBy { it.periode.fom }
            .filterNot { it.erAvslåttPåKvote() }
            .somTidslinje { it.periode }.map { it.rettighetstype }
            .komprimer()
    }
}