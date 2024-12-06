package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import java.time.LocalDate

class HåndterMottattDokumentService(connection: DBConnection) {

    private val sakService = SakService(SakRepositoryImpl(connection))
    private val sakOgBehandlingService = SakOgBehandlingService(connection)
    private val låsRepository = TaSkriveLåsRepositoryImpl(connection)
    private val prosesserBehandling = ProsesserBehandlingService(FlytJobbRepository(connection))

    fun håndterMottatteDokumenter(
        sakId: SakId,
        brevkategori: InnsendingType,
        periode: Periode?,
        mottattDato: LocalDate
    ) {

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

    private fun utledÅrsak(brevkategori: InnsendingType, periode: Periode?): Årsak {
        return when (brevkategori) {
            InnsendingType.SØKNAD -> Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD)
            InnsendingType.PLIKTKORT ->
                Årsak(
                    ÅrsakTilBehandling.MOTTATT_MELDEKORT,
                    periode
                )

            InnsendingType.AKTIVITETSKORT -> Årsak(ÅrsakTilBehandling.MOTTATT_AKTIVITETSMELDING, periode)
            InnsendingType.LEGEERKLÆRING_AVVIST -> Årsak(ÅrsakTilBehandling.MOTTATT_AVVIST_LEGEERKLÆRING)
            InnsendingType.LEGEERKLÆRING -> Årsak(ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING)
            InnsendingType.DIALOGMELDING -> Årsak(ÅrsakTilBehandling.MOTTATT_DIALOGMELDING)
        }
    }
}