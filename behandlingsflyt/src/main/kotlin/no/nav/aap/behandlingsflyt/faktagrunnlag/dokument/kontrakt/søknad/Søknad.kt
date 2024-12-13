package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.søknad

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.ErStudentStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.SkalGjenopptaStudieStatus

// duplisert
class Søknad(
    val student: SøknadStudentDto,
    val yrkesskade: String,
    val oppgitteBarn: OppgitteBarn?
) {
    // TODO: Håndtere IKKE_OPPGITT?
    fun harYrkesskade(): Boolean {
        return yrkesskade.uppercase() == "JA"
    }
}

class SøknadStudentDto(
    val erStudent: String,
    val kommeTilbake: String? = null
) {
    fun erStudent(): ErStudentStatus? {
        return if (erStudent.uppercase() == "JA"){
            ErStudentStatus.JA
        } else if (erStudent.uppercase() == "AVBRUTT") {
            ErStudentStatus.AVBRUTT
        } else if (erStudent.uppercase() == "NEI") {
            ErStudentStatus.NEI
        } else {
            null
        }
    }

    fun skalGjennopptaStudie(): SkalGjenopptaStudieStatus {
        return if (kommeTilbake?.uppercase() == "JA"){
            SkalGjenopptaStudieStatus.JA
        } else if (kommeTilbake?.uppercase() == "NEI"){
            SkalGjenopptaStudieStatus.NEI
        } else if (kommeTilbake?.uppercase() == "VET IKKE"){
            SkalGjenopptaStudieStatus.VET_IKKE
        } else {
            SkalGjenopptaStudieStatus.IKKE_OPPGITT
        }
    }
}
