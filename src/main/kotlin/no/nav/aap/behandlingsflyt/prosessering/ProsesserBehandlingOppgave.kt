package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator

object ProsesserBehandlingOppgave : Oppgave() {

    private val kontroller = FlytOrkestrator(Faktagrunnlag())

    override fun utfør(input: OppgaveInput) {
        kontroller.prosesserBehandling(FlytKontekst(sakId = input.sakId(), behandlingId = input.behandlingId()))
    }

    override fun type(): String {
        return "flyt.prosesserBehandling"
    }
}