package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.dbstuff.DbConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator

object ProsesserBehandlingOppgave : Oppgave() {

    override fun utfør(connection: DbConnection, input: OppgaveInput) {
        val kontroller = FlytOrkestrator(Faktagrunnlag(), connection)
        kontroller.forberedBehandling(FlytKontekst(sakId = input.sakId(), behandlingId = input.behandlingId()))
        kontroller.prosesserBehandling(FlytKontekst(sakId = input.sakId(), behandlingId = input.behandlingId()))
    }

    override fun type(): String {
        return "flyt.prosesserBehandling"
    }
}