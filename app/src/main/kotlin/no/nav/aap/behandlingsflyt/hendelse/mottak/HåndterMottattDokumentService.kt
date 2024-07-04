package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.EndringType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.server.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.SakId

class HåndterMottattDokumentService(connection: DBConnection) {

    private val sakService = SakService(connection)
    private val sakOgBehandlingService = SakOgBehandlingService(connection)
    private val låsRepository = TaSkriveLåsRepository(connection)
    private val flytJobbRepository = FlytJobbRepository(connection)

    fun håndterMottatteDokumenter(sakId: SakId, brevkode: Brevkode, periode: Periode?) {

        val sak = sakService.hent(sakId)
        val beriketBehandling =
            sakOgBehandlingService.finnEllerOpprettBehandling(sak.saksnummer, listOf(utledÅrsak(brevkode, periode)))

        val behandlingSkrivelås = låsRepository.låsBehandling(beriketBehandling.behandling.id)

        // Skal da planlegge ny jobb
        flytJobbRepository.leggTil(
            JobbInput(jobb = ProsesserBehandlingJobbUtfører).forBehandling(
                sakId,
                beriketBehandling.behandling.id
            ).medCallId()
        )

        låsRepository.verifiserSkrivelås(behandlingSkrivelås)
    }

    private fun utledÅrsak(brevkode: Brevkode, periode: Periode?): Årsak {
        return when (brevkode) {
            Brevkode.SØKNAD -> Årsak(EndringType.MOTTATT_SØKNAD)
            Brevkode.PLIKTKORT ->
                Årsak(
                    EndringType.MOTTATT_MELDEKORT,
                    periode
                )

            Brevkode.AKTIVITETSKORT -> Årsak(EndringType.MOTTATT_AKTIVITETSMELDING, periode)
            Brevkode.UKJENT -> TODO("Ukjent dokument")
        }
    }
}