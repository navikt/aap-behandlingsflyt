package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student

import java.time.LocalDate

class OppgittStudent(
    val id: Long? = null,
    val avbruttDato: LocalDate? = null,
    val erStudentStatus: ErStudentStatus,
    val skalGjenopptaStudieStatus: SkalGjenopptaStudieStatus? = null
) {
    fun erStudent() = erStudentStatus == ErStudentStatus.JA || erStudentStatus == ErStudentStatus.AVBRUTT
}
