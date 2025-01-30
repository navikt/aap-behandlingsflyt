package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Aktivitetskort
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AktivitetskortV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AnnetRelevantDokumentV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Pliktkort
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.PliktkortV0
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.tilÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.LocalDateTime

class HåndterMottattDokumentService(
    private val sakService: SakService,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val låsRepository: TaSkriveLåsRepository,
    private val prosesserBehandling: ProsesserBehandlingService,
) {

    fun håndterMottatteDokumenter(
        sakId: SakId,
        mottattTidspunkt: LocalDateTime,
        brevkategori: InnsendingType,
        melding: Melding?,
        mottattDato: LocalDate
    ) {
        val sak = sakService.hent(sakId)
        val periode = utledPeriode(brevkategori, mottattTidspunkt, melding)
        val element = utledÅrsak(brevkategori, melding, periode)
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

    private fun utledÅrsak(brevkategori: InnsendingType, melding: Melding?, periode: Periode?): Årsak {
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
            InnsendingType.ANNET_RELEVANT_DOKUMENT ->
                when (melding) {
                    is AnnetRelevantDokumentV0 -> Årsak(melding.årsakTilBehandling.tilÅrsakTilBehandling())
                    else -> error("Melding må være AnnetRelevantDokumentV0")
                }

        }
    }

    private fun utledPeriode(
        innsendingType: InnsendingType,
        mottattTidspunkt: LocalDateTime,
        melding: Melding?,
    ): Periode? {
        return when (innsendingType) {
            InnsendingType.SØKNAD -> Periode(
                mottattTidspunkt.toLocalDate(),
                mottattTidspunkt.plusYears(1).toLocalDate()
            )

            InnsendingType.AKTIVITETSKORT -> if (melding is Aktivitetskort) {
                when (melding) {
                    is AktivitetskortV0 -> Periode(fom = melding.fraOgMed, tom = melding.tilOgMed)
                }

            } else error("Må være aktivitetskort")

            InnsendingType.PLIKTKORT -> if (melding is Pliktkort) {
                when (melding) {
                    is PliktkortV0 -> Periode(
                        fom = melding.fom() ?: mottattTidspunkt.toLocalDate(),
                        tom = melding.tom() ?: mottattTidspunkt.toLocalDate()
                    )
                }
            } else error("Må være Pliktkort")

            else -> null
        }
    }
}