package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.type.Periode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

class HåndterMottattDokumentService(
    private val sakService: SakService,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val låsRepository: TaSkriveLåsRepository,
    private val prosesserBehandling: ProsesserBehandlingService,
    private val templogger: Logger = LoggerFactory.getLogger(HåndterMottattDokumentService::class.java)
) {

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

        templogger.info("kjørte HåndterMottattDokumentService verifiserSkrivelås")
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