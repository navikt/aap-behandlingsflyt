package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

data class StudentGrunnlag(
    val vurderinger: Set<StudentVurdering>?,
    val oppgittStudent: OppgittStudent?
) {
    fun somStudenttidslinje(maksDato: LocalDate = Tid.MAKS): Tidslinje<StudentVurdering> {
        return filtrertStudenttidslinje(maksDato) { true }
    }

    fun gjeldendeStudentvurderinger(maksDato: LocalDate = Tid.MAKS): List<StudentVurdering> {
        return somStudenttidslinje(maksDato).segmenter().map { it.verdi }
    }

    fun studentvurderingerVurdertIBehandling(behandlingId: BehandlingId): List<StudentVurdering> {
        return vurderinger.orEmpty().filter { it.vurdertIBehandling == behandlingId }
    }

    fun historiskeStudentvurderinger(behandlingIdForGrunnlag: BehandlingId): List<StudentVurdering> {
        return vurderinger.orEmpty().filterNot { it.vurdertIBehandling == behandlingIdForGrunnlag }
    }

    fun vedtattStudenttidslinje(
        behandlingId: BehandlingId,
        maksDato: LocalDate = Tid.MAKS,
    ): Tidslinje<StudentVurdering> {
        return filtrertStudenttidslinje(maksDato) { it.vurdertIBehandling != behandlingId }
    }

    private fun filtrertStudenttidslinje(
        maksDato: LocalDate = Tid.MAKS,
        filter: (studentvurdering: StudentVurdering) -> Boolean
    ): Tidslinje<StudentVurdering> {
        return vurderinger.orEmpty()
            .filter(filter)
            .groupBy { it.vurdertIBehandling }
            .values
            .sortedBy { it[0].vurdertTidspunkt }
            .flatMap { it.sortedBy { it.fom } }
            .somTidslinje { Periode(it.fom, it.tom ?: maksDato) }
    }
}

fun StudentGrunnlag?.harPeriodeSomIkkeErOppfylt(maksDato: LocalDate = Tid.MAKS): Boolean {
    val tidslinje = this?.somStudenttidslinje(maksDato).orEmpty()
    if (tidslinje.isEmpty()) {
        return true
    }
    return tidslinje.segmenter().any { !it.verdi.erOppfylt() }
}

fun StudentGrunnlag?.søkerOppgirStudentstatus(): Boolean {
    return this?.oppgittStudent?.erStudent() == true
}