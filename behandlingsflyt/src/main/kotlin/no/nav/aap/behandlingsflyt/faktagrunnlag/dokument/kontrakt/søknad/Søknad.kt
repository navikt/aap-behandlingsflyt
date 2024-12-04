package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.søknad

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.ErStudentStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.SkalGjenopptaStudieStatus

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
    fun erStudent(): ErStudentStatus {
      if (erStudent.uppercase() == "JA"){
          return ErStudentStatus.JA
      } else if (erStudent.uppercase() == "AVBRUTT") {
          return ErStudentStatus.AVBRUTT
      } else if (erStudent.uppercase() == "NEI") {
          return ErStudentStatus.NEI
      } else {
          return ErStudentStatus.IKKE_OPPGITT
      }
    }

    fun skalGjennopptaStudie(): SkalGjenopptaStudieStatus {
        if (kommeTilbake?.uppercase() == "JA"){
            return SkalGjenopptaStudieStatus.JA
        } else if (kommeTilbake?.uppercase() == "NEI"){
            return SkalGjenopptaStudieStatus.NEI
        } else if (kommeTilbake?.uppercase() == "VET IKKE"){
            return SkalGjenopptaStudieStatus.VET_IKKE
        } else {
            return SkalGjenopptaStudieStatus.IKKE_OPPGITT
        }
    }
}
