package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.server.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.verdityper.flyt.ÅrsakTilBehandling
import no.nav.aap.verdityper.sakogbehandling.SakId

class HåndterMottattDokumentService(connection: DBConnection) {

    private val sakService = SakService(connection)
    private val sakOgBehandlingService = SakOgBehandlingService(connection)
    private val låsRepository = TaSkriveLåsRepository(connection)
    private val flytJobbRepository = FlytJobbRepository(connection)

    fun håndterMottatteDokumenter(sakId: SakId, brevkode: Brevkode, periode: Periode?) {

        val sak = sakService.hent(sakId)
        val element = utledÅrsak(brevkode, periode)
        val beriketBehandling =
            sakOgBehandlingService.finnEllerOpprettBehandling(sak.saksnummer, listOf(element))

        val behandlingSkrivelås = låsRepository.låsBehandling(beriketBehandling.behandling.id)

        // Skal da planlegge ny jobb
        flytJobbRepository.leggTil(
            JobbInput(jobb = ProsesserBehandlingJobbUtfører).forBehandling(
                sakId.toLong(),
                beriketBehandling.behandling.id.toLong()
            ).medParameter("trigger", element.type.name).medCallId()
        )

        låsRepository.verifiserSkrivelås(behandlingSkrivelås)
    }

    private fun utledÅrsak(brevkode: Brevkode, periode: Periode?): Årsak {
        return when (brevkode) {
            Brevkode.SØKNAD -> Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD)
            Brevkode.PLIKTKORT ->
                Årsak(
                    ÅrsakTilBehandling.MOTTATT_MELDEKORT,
                    periode
                )

            Brevkode.AKTIVITETSKORT -> Årsak(ÅrsakTilBehandling.MOTTATT_AKTIVITETSMELDING, periode)
            Brevkode.UKJENT -> TODO("Ukjent dokument")
        }
    }
}