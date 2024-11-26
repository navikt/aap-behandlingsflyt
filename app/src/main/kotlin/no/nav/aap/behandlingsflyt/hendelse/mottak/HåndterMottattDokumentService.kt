package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.Brevkategori
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.server.prosessering.ProsesserBehandlingService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.verdityper.flyt.ÅrsakTilBehandling
import no.nav.aap.verdityper.sakogbehandling.SakId
import java.time.LocalDate

class HåndterMottattDokumentService(connection: DBConnection) {

    private val sakService = SakService(SakRepositoryImpl(connection))
    private val sakOgBehandlingService = SakOgBehandlingService(connection)
    private val låsRepository = TaSkriveLåsRepository(connection)
    private val prosesserBehandling = ProsesserBehandlingService(FlytJobbRepository(connection))

    fun håndterMottatteDokumenter(sakId: SakId, brevkategori: Brevkategori, periode: Periode?, mottattDato: LocalDate) {

        val sak = sakService.hent(sakId)
        val element = utledÅrsak(brevkategori, periode)
        val beriketBehandling =
            sakOgBehandlingService.finnEllerOpprettBehandling(sak.saksnummer, listOf(element))

        val behandlingSkrivelås = låsRepository.låsBehandling(beriketBehandling.behandling.id)

        sakOgBehandlingService.oppdaterRettighetsperioden(sakId, brevkategori, mottattDato)

        prosesserBehandling.triggProsesserBehandling(
            sakId,
            beriketBehandling.behandling.id,
            listOf("trigger" to element.type.name)
        )

        låsRepository.verifiserSkrivelås(behandlingSkrivelås)
    }

    private fun utledÅrsak(brevkategori: Brevkategori, periode: Periode?): Årsak {
        return when (brevkategori) {
            Brevkategori.SØKNAD -> Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD)
            Brevkategori.PLIKTKORT ->
                Årsak(
                    ÅrsakTilBehandling.MOTTATT_MELDEKORT,
                    periode
                )

            Brevkategori.AKTIVITETSKORT -> Årsak(ÅrsakTilBehandling.MOTTATT_AKTIVITETSMELDING, periode)
            Brevkategori.UKJENT -> TODO("Ukjent dokument")
            Brevkategori.LEGEERKLÆRING_AVVIST -> Årsak(ÅrsakTilBehandling.MOTTATT_AVVIST_LEGEERKLÆRING)
            Brevkategori.LEGEERKLÆRING_MOTTATT -> Årsak(ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING)
            Brevkategori.DIALOGMELDING -> Årsak(ÅrsakTilBehandling.MOTTATT_DIALOGMELDING)
        }
    }
}