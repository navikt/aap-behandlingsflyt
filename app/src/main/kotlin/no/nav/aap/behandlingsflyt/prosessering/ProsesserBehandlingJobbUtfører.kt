package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.verdityper.flyt.FlytKontekst

class ProsesserBehandlingJobbUtfører(
    private val låsRepository: TaSkriveLåsRepository,
    private val kontroller: FlytOrkestrator
) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val skrivelås = låsRepository.lås(input.sakId(), input.behandlingId())

        val kontekst = FlytKontekst(
            sakId = input.sakId(),
            behandlingId = input.behandlingId(),
            behandlingType = skrivelås.behandlingSkrivelås.typeBehandling
        )
        kontroller.forberedBehandling(kontekst)
        kontroller.prosesserBehandling(kontekst)

        låsRepository.verifiserSkrivelås(skrivelås)
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return ProsesserBehandlingJobbUtfører(TaSkriveLåsRepository(connection), FlytOrkestrator(connection), )
        }

        override fun type(): String {
            return "flyt.prosesserBehandling"
        }
    }
}
