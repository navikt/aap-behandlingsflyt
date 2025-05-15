package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Aktivitetskort
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AktivitetskortV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AnnetRelevantDokumentV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Klage
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurderingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Meldekort
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.NyÅrsakTilBehandlingV0
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.tilÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDateTime

class HåndterMottattDokumentService(
    private val sakService: SakService,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val låsRepository: TaSkriveLåsRepository,
    private val prosesserBehandling: ProsesserBehandlingService,
    private val mottaDokumentService: MottaDokumentService
) {

    constructor(repositoryProvider: RepositoryProvider) : this(
        sakService = SakService(repositoryProvider),
        sakOgBehandlingService = SakOgBehandlingService(repositoryProvider),
        låsRepository = repositoryProvider.provide(),
        prosesserBehandling = ProsesserBehandlingService(repositoryProvider),
        mottaDokumentService = MottaDokumentService(repositoryProvider)
    )

    fun håndterMottatteDokumenter(
        sakId: SakId,
        referanse: InnsendingReferanse,
        mottattTidspunkt: LocalDateTime,
        brevkategori: InnsendingType,
        melding: Melding?,
    ) {
        val sak = sakService.hent(sakId)
        val periode = utledPeriode(brevkategori, mottattTidspunkt, melding)
        val årsaker = utledÅrsaker(brevkategori, melding, periode)

        val beriketBehandling = sakOgBehandlingService.finnEllerOpprettBehandling(sak.saksnummer, årsaker)

        // TODO: Evaluer at at behandlingen faktisk kan motta endringene
        // Står hos beslutter - Hvilke endringer kan da håndteres
        // P.d.d. ingen da de feilaktig kobles på behandling men ikke tas hensyn til

        val behandlingSkrivelås = låsRepository.låsBehandling(beriketBehandling.behandling.id)

        sakOgBehandlingService.oppdaterRettighetsperioden(sakId, brevkategori, mottattTidspunkt.toLocalDate())

        // Knytter klage direkte til behandlingen den opprettet, i stedet for via informasjonskrav.
        // Dette fordi vi kan ha flere åpne klagebehandlinger.
        if (melding is Klage) {
            mottaDokumentService.knyttTilBehandling(sakId, beriketBehandling.behandling.id, referanse)
        }

        prosesserBehandling.triggProsesserBehandling(
            sakId,
            beriketBehandling.behandling.id,
            listOf("trigger" to årsaker.map { it.type.name }.toString())
        )
        låsRepository.verifiserSkrivelås(behandlingSkrivelås)
    }

    fun oppdaterÅrsakerTilBehandlingPåEksisterendeÅpenBehandling(sakId: SakId, behandlingsreferanse: BehandlingReferanse, melding: NyÅrsakTilBehandlingV0) {
        val behandling = sakOgBehandlingService.finnBehandling(behandlingsreferanse)

        låsRepository.withLåstBehandling(behandling.id) {
            val årsaker = melding.årsakerTilBehandling.map { Årsak(it.tilÅrsakTilBehandling()) }
            sakOgBehandlingService.oppdaterÅrsakerTilBehandling(behandling, årsaker)

            prosesserBehandling.triggProsesserBehandling(
                sakId,
                behandling.id,
                listOf("trigger" to årsaker.map { it.type.name }.toString())
            )
        }
    }


    private fun utledÅrsaker(brevkategori: InnsendingType, melding: Melding?, periode: Periode?): List<Årsak> {
        return when (brevkategori) {
            InnsendingType.SØKNAD -> listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD))
            InnsendingType.MANUELL_REVURDERING -> when (melding) {
                is ManuellRevurderingV0 -> melding.årsakerTilBehandling.map { Årsak(it.tilÅrsakTilBehandling()) }
                else -> error("Melding må være ManuellRevurderingV0")
            }

            InnsendingType.MELDEKORT ->
                listOf(
                    Årsak(
                        ÅrsakTilBehandling.MOTTATT_MELDEKORT,
                        periode
                    )
                )

            InnsendingType.AKTIVITETSKORT -> listOf(Årsak(ÅrsakTilBehandling.MOTTATT_AKTIVITETSMELDING, periode))
            InnsendingType.LEGEERKLÆRING_AVVIST -> listOf(Årsak(ÅrsakTilBehandling.MOTTATT_AVVIST_LEGEERKLÆRING))
            InnsendingType.LEGEERKLÆRING -> listOf(Årsak(ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING))
            InnsendingType.DIALOGMELDING -> listOf(Årsak(ÅrsakTilBehandling.MOTTATT_DIALOGMELDING))
            InnsendingType.ANNET_RELEVANT_DOKUMENT ->
                when (melding) {
                    is AnnetRelevantDokumentV0 -> melding.årsakerTilBehandling.map { Årsak(it.tilÅrsakTilBehandling()) }
                    else -> error("Melding må være AnnetRelevantDokumentV0")
                }

            InnsendingType.KLAGE -> listOf(Årsak(ÅrsakTilBehandling.MOTATT_KLAGE))
            InnsendingType.NY_ÅRSAK_TIL_BEHANDLING ->
                when (melding) {
                    is NyÅrsakTilBehandlingV0 -> melding.årsakerTilBehandling.map { Årsak(it.tilÅrsakTilBehandling()) }
                    else -> error("Melding må være NyÅrsakTilBehandlingV0")
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

            InnsendingType.MELDEKORT -> if (melding is Meldekort) {
                when (melding) {
                    is MeldekortV0 -> Periode(
                        fom = melding.fom() ?: mottattTidspunkt.toLocalDate(),
                        tom = melding.tom() ?: mottattTidspunkt.toLocalDate()
                    )
                }
            } else error("Må være meldekort")

            else -> null
        }
    }
}