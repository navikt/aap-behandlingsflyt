package no.nav.aap.prosessering

import no.nav.aap.flyt.kontroll.FlytKontekst
import no.nav.aap.flyt.kontroll.FlytKontroller

object ProsesserBehandlingOppgave : Oppgave() {

    private val kontroller = FlytKontroller()

    override fun utfør(input: OppgaveInput) {
        kontroller.prosesserBehandling(FlytKontekst(sakId = input.sakId(), behandlingId = input.behandlingId()))
    }

    override fun type(): String {
        return "flyt.prosesserBehandling"
    }
}