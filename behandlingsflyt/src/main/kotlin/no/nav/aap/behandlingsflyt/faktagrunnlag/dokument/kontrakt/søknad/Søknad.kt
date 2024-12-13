package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.søknad

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.ErStudentStatus

// duplisert
@Deprecated(
    "Bruk Innsending-objekt i stedet. Og et annet endepunkt.", replaceWith = ReplaceWith(
        expression = "no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0"
    )
)
class Søknad(
    val student: SøknadStudentDto?,
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
        return if (erStudent.uppercase() == "JA") {
            ErStudentStatus.JA
        } else if (erStudent.uppercase() == "AVBRUTT") {
            ErStudentStatus.AVBRUTT
        } else if (erStudent.uppercase() == "NEI") {
            ErStudentStatus.NEI
        } else {
            null
        }
    }

}
