package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.flyt.kontroll.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.kontroll.FlytOrkestrator

object ProsesserBehandlingOppgave : Oppgave() {

    private val kontroller = FlytOrkestrator()

    override fun utfør(input: OppgaveInput) {
        kontroller.prosesserBehandling(FlytKontekst(sakId = input.sakId(), behandlingId = input.behandlingId()))
    }

    override fun type(): String {
        return "flyt.prosesserBehandling"
    }
}