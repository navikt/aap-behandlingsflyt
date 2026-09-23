package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje

data class StudentGrunnlag(
    val vurderinger: Set<StudentVurdering>?,
    val oppgittStudent: OppgittStudent?
) {
    fun somStudenttidslinje(): Tidslinje<StudentVurdering> {
        return filtrertStudenttidslinje { true }
    }

    fun gjeldendeStudentvurderinger(): List<StudentVurdering> {
        return somStudenttidslinje().segmenter().map { it.verdi }
    }

    fun studentvurderingerVurdertIBehandling(behandlingId: BehandlingId): List<StudentVurdering> {
        return vurderinger.orEmpty().filter { it.vurdertIBehandling == behandlingId }
    }

    fun vedtattStudenttidslinje(
        behandlingId: BehandlingId,
    ): Tidslinje<StudentVurdering> {
        return filtrertStudenttidslinje { it.vurdertIBehandling != behandlingId }
    }

    private fun filtrertStudenttidslinje(
        filter: (studentvurdering: StudentVurdering) -> Boolean
    ): Tidslinje<StudentVurdering> {
        return vurderinger.orEmpty()
            .filter(filter)
            .gjeldendeVurderinger()
    }
}

fun StudentGrunnlag?.skalVurdereStudent(): Boolean {
    return this?.oppgittStudent?.erStudentStatus == ErStudentStatus.AVBRUTT
            && this.oppgittStudent.skalGjenopptaStudieStatus in listOf(
        SkalGjenopptaStudieStatus.JA,
        SkalGjenopptaStudieStatus.VET_IKKE
    )
}