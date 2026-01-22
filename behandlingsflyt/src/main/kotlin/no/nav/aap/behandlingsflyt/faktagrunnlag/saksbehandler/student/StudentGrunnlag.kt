package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid

data class StudentGrunnlag(
    val vurderinger: Set<StudentVurdering>?,
    val oppgittStudent: OppgittStudent?
) {
    fun somStudenttidslinje(rettighetsperiode: Periode): Tidslinje<StudentVurdering> {
        return filtrertStudenttidslinje(rettighetsperiode) { true }
    }

    fun gjeldendeStudentvurderinger(rettighetsperiode: Periode): List<StudentVurdering> {
        return somStudenttidslinje(rettighetsperiode).segmenter().map { it.verdi }
    }

    fun studentvurderingerVurdertIBehandling(behandlingId: BehandlingId): List<StudentVurdering> {
        return vurderinger.orEmpty().filter { it.vurdertIBehandling == behandlingId }
    }

    fun historiskeStudentvurderinger(behandlingIdForGrunnlag: BehandlingId): List<StudentVurdering> {
        return vurderinger.orEmpty().filterNot { it.vurdertIBehandling == behandlingIdForGrunnlag }
    }

    fun vedtattStudenttidslinje(
        rettighetsperiode: Periode,
        behandlingId: BehandlingId
    ): Tidslinje<StudentVurdering> {
        return filtrertStudenttidslinje(rettighetsperiode) { it.vurdertIBehandling != behandlingId }
    }

    private fun filtrertStudenttidslinje(
        rettighetsperiode: Periode,
        filter: (studentvurdering: StudentVurdering) -> Boolean
    ): Tidslinje<StudentVurdering> {
        return vurderinger.orEmpty()
            .filter(filter)
            .groupBy { it.vurdertIBehandling }
            .values
            .sortedBy { it[0].vurdertTidspunkt }
            .flatMap { it.sortedBy { it.fom ?: rettighetsperiode.fom } }
            .somTidslinje { Periode(it.fom ?: rettighetsperiode.fom, rettighetsperiode.tom) }
    }
}

fun StudentGrunnlag?.harPeriodeSomIkkeErOppfylt(): Boolean {
    val tidslinje = this?.somStudenttidslinje(Periode(Tid.MIN, Tid.MAKS)).orEmpty()
    if (tidslinje.isEmpty()) {
        return true
    }
    return tidslinje.segmenter().any { !it.verdi.erOppfylt() }
}

fun StudentGrunnlag?.søkerOppgirStudentstatus(): Boolean {
    return this?.oppgittStudent?.erStudent() == true
}