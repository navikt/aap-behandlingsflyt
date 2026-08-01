package no.nav.aap.behandlingsflyt.steg.iverksett

import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.misc.Ident
import java.time.LocalDate

interface GosysOppgaveGateway : Gateway {

    fun opprettOppgave(
        oppgavetype: OppgaveType,
        tema: String = "AAP",
        personIdent: Ident,
        bestillingReferanse: String,
        tildeltEnhetsnr: String,
        opprettetAvEnhetsnr: String,
        behandlingstema: Behandlingstema,
        beskrivelse: String,
        prioritet: Prioritet = Prioritet.NORM,
        aktivDato: LocalDate = LocalDate.now(),
        fristFerdigstillelse: LocalDate? = null,
    )
}

enum class Behandlingstema(val kode: String) {
    REFUSJON("ab0504");
}

enum class OppgaveType(val verdi: String) {
    VURDER_KONSEKVENS_FOR_YTELSE("VUR_KONS_YTE"),
}

