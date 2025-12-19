package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student

import java.time.LocalDate

class OppgittStudent(
    val id: Long? = null,
    val avbruttDato: LocalDate? = null,
    val erStudentStatus: ErStudentStatus,
    val skalGjenopptaStudieStatus: SkalGjenopptaStudieStatus? = null
) {
    fun erStudent() = erStudentStatus == ErStudentStatus.JA || erStudentStatus == ErStudentStatus.AVBRUTT

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OppgittStudent
        
        if (avbruttDato != other.avbruttDato) return false
        if (erStudentStatus != other.erStudentStatus) return false
        if (skalGjenopptaStudieStatus != other.skalGjenopptaStudieStatus) return false

        return true
    }

    override fun hashCode(): Int {
        var result = avbruttDato?.hashCode() ?: 0
        result = 31 * result + erStudentStatus.hashCode()
        result = 31 * result + (skalGjenopptaStudieStatus?.hashCode() ?: 0)
        return result
    }
    
}
