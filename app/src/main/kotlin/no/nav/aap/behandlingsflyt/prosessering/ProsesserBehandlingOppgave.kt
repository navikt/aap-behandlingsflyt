package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator

object ProsesserBehandlingOppgave : Oppgave() {

    override fun utfør(connection: DBConnection, input: OppgaveInput) {
        val låsRepository = TaSkriveLåsRepository(connection)
        val skrivelås = låsRepository.lås(input.sakId(), input.behandlingId())

        val kontroller = FlytOrkestrator(connection)

        val kontekst = FlytKontekst(
            sakId = input.sakId(),
            behandlingId = input.behandlingId(),
            behandlingType = skrivelås.behandlingSkrivelås.typeBehandling
        )
        kontroller.forberedBehandling(kontekst)
        kontroller.prosesserBehandling(kontekst)

        låsRepository.verifiserSkrivelås(skrivelås)
    }

    override fun type(): String {
        return "flyt.prosesserBehandling"
    }
}
