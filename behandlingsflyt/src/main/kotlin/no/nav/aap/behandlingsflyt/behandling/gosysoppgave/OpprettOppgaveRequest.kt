package no.nav.aap.behandlingsflyt.behandling.gosysoppgave

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.ZoneId.systemDefault
import java.util.*

data class OpprettOppgaveRequest(
    val oppgavetype: String, // se kodeverk
    val tema: String = "AAP", // se kodeverk
    val prioritet: Prioritet = Prioritet.NORM,
    val aktivDato: String = LocalDate.now().toString(), // dato
    val personident: String? = null, // 11 - 13 tegn
    val orgnr: String? = null,
    val tildeltEnhetsnr: NavEnhet? = null, // 4 tegn
    val opprettetAvEnhetsnr: NavEnhet? = null, // 4 tegn
    val behandlesAvApplikasjon: String? = null,
    val tilordnetRessurs: String? = null, // navident
    val beskrivelse: String? = null,
    val behandlingstema: String? = null, // se kodeverk
    val behandlingstype: String? = null, // se kodeverk
    val fristFerdigstillelse: LocalDate? = null
)

typealias NavEnhet = String

enum class Prioritet {
    HOY,
    NORM,
    LAV
}