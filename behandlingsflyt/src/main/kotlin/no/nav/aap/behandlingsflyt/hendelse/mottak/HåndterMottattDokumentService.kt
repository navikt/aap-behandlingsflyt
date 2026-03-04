package no.nav.aap.behandlingsflyt.hendelse.mottak

import java.time.LocalDateTime
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AnnetRelevantDokument
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KabalHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Klage
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurdering
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurderingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.NyÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.NyÅrsakTilBehandlingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OmgjøringKlageRevurdering
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Omgjøringskilde
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Oppfølgingsoppgave
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OppfølgingsoppgaveV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.PdlHendelseV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV0
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.tilVurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class HåndterMottattDokumentService(
    private val sakService: SakService,
    private val behandlingService: BehandlingService,
    private val låsRepository: TaSkriveLåsRepository,
    private val prosesserBehandling: ProsesserBehandlingService,
    private val mottaDokumentService: MottaDokumentService,
    private val behandlingRepository: BehandlingRepository,
    private val håndterKlageService: HåndterKlageService,
    private val håndterDialogMeldingService: HåndterDialogMeldingService,
    private val håndterTilbakekrevingHendelseService: HåndterTilbakekrevingHendelseService,
    private val håndterSykepengevedtakService: HåndterSykepengevedtakService,
    private val håndterUførevedtakService: HåndterUførevedtakService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sakService = SakService(repositoryProvider, gatewayProvider),
        behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
        låsRepository = repositoryProvider.provide(),
        prosesserBehandling = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
        mottaDokumentService = MottaDokumentService(repositoryProvider),
        behandlingRepository = repositoryProvider.provide<BehandlingRepository>(),
        håndterKlageService = HåndterKlageService(repositoryProvider, gatewayProvider),
        håndterDialogMeldingService = HåndterDialogMeldingService(repositoryProvider, gatewayProvider),
        håndterTilbakekrevingHendelseService = HåndterTilbakekrevingHendelseService(repositoryProvider, gatewayProvider),
        håndterSykepengevedtakService = HåndterSykepengevedtakService(repositoryProvider, gatewayProvider),
        håndterUførevedtakService = HåndterUførevedtakService(repositoryProvider, gatewayProvider),
    )

    fun håndterMottatteDokumenter(
        sakId: SakId,
        referanse: InnsendingReferanse,
        mottattTidspunkt: LocalDateTime,
        brevkategori: InnsendingType,
        melding: Melding?,
    ) {
        log.info("Mottok dokument på sak-id $sakId, og referanse $referanse, med brevkategori $brevkategori.")
        val sak = sakService.hent(sakId)
        val vurderingsbehov = utledVurderingsbehov(brevkategori, melding)
        val årsakTilOpprettelse = utledÅrsakTilOpprettelse(brevkategori, melding)

        val opprettetBehandling = behandlingService.finnEllerOpprettBehandling(
            sak.saksnummer,
            VurderingsbehovOgÅrsak(
                årsak = årsakTilOpprettelse,
                vurderingsbehov = vurderingsbehov,
                opprettet = mottattTidspunkt,
                beskrivelse = utledBeskrivelseForÅrsakTilOpprettelse(melding)
            )
        )

        val behandlingSkrivelås = opprettetBehandling.åpenBehandling?.let {
            låsRepository.låsBehandling(it.id)
        }

        sakService.oppdaterRettighetsperioden(sakId, brevkategori, mottattTidspunkt.toLocalDate())

        if (skalMarkereDokumentSomBehandlet(melding)) {
            require(opprettetBehandling is BehandlingService.Ordinær) {
                "Forventet ordinær behandling ved mottak av dokumenter som skal markeres som behandlet"
            }
            mottaDokumentService.markerSomBehandlet(sakId, opprettetBehandling.åpenBehandling.id, referanse)
        } else {
            knyttDokumentTilRiktigBehandling(opprettetBehandling, sakId, referanse)
        }

        prosesserBehandling.triggProsesserBehandling(
            opprettetBehandling,
            vurderingsbehov = vurderingsbehov.map { it.type }
        )

        if (behandlingSkrivelås != null) {
            låsRepository.verifiserSkrivelås(behandlingSkrivelås)
        }
    }

    fun håndterMottatteKlage(
        sakId: SakId,
        referanse: InnsendingReferanse,
        mottattTidspunkt: LocalDateTime,
        brevkategori: InnsendingType,
        melding: Klage,
    ) {
        håndterKlageService.håndterMottatteKlage(
            sakId = sakId,
            referanse = referanse,
            mottattTidspunkt = mottattTidspunkt,
            brevkategori = brevkategori,
            melding = melding,
            vurderingsbehov = utledVurderingsbehov(brevkategori, melding)
        )
    }

    fun håndterMottattOmgjøringEtterKlage(
        sakId: SakId,
        referanse: InnsendingReferanse,
        mottattTidspunkt: LocalDateTime,
        brevkategori: InnsendingType,
        melding: OmgjøringKlageRevurdering,
    ) {
        håndterKlageService.håndterMottattOmgjøringEtterKlage(
            sakId = sakId,
            referanse = referanse,
            mottattTidspunkt = mottattTidspunkt,
            melding = melding,
            vurderingsbehov = utledVurderingsbehov(brevkategori, melding),
            årsakTilOpprettelse = utledÅrsakTilOpprettelse(brevkategori, melding),
        )
    }

    fun håndterMottattDialogMelding(
        sakId: SakId,
        referanse: InnsendingReferanse,
        brevkategori: InnsendingType,
        melding: Melding?
    ) {
        håndterDialogMeldingService.håndterMottattDialogMelding(
            sakId = sakId,
            referanse = referanse,
            vurderingsbehov = utledVurderingsbehov(brevkategori, melding)
        )
    }

    fun håndterMottattTilbakekrevingHendelse(sakId: SakId, referanse: InnsendingReferanse, melding: TilbakekrevingHendelse) {
        håndterTilbakekrevingHendelseService.håndterMottattTilbakekrevingHendelse(
            sakId = sakId,
            referanse = referanse,
            melding = melding
        )
    }

    fun håndterMottattSykepengevedtakHendelse(sakId: SakId, referanse: InnsendingReferanse) {
        håndterSykepengevedtakService.håndterMottattSykepengevedtakHendelse(sakId = sakId, referanse = referanse)
    }

    fun håndterMottattUførevedtakHendelse(
        sakId: SakId,
        referanse: InnsendingReferanse,
        uførevedtak: UførevedtakV0,
        mottattTidspunkt: LocalDateTime
    ) {
        håndterUførevedtakService.håndterMottattUførevedtakHendelse(
            sakId = sakId,
            referanse = referanse,
            uførevedtak = uførevedtak,
            mottattTidspunkt = mottattTidspunkt
        )
    }


    fun oppdaterÅrsakerTilBehandlingPåEksisterendeÅpenBehandling(
        sakId: SakId,
        behandlingsreferanse: BehandlingReferanse,
        innsendingType: InnsendingType,
        melding: NyÅrsakTilBehandlingV0,
        referanse: InnsendingReferanse
    ) {
        val behandling = behandlingRepository.hent(behandlingsreferanse)
        val årsakTilOpprettelse = utledÅrsakTilOpprettelse(innsendingType, melding)

        låsRepository.withLåstBehandling(behandling.id) {
            val vurderingsbehov =
                melding.årsakerTilBehandling.map { VurderingsbehovMedPeriode(it.tilVurderingsbehov()) }
            behandlingService.oppdaterVurderingsbehovOgÅrsak(
                behandling,
                VurderingsbehovOgÅrsak(vurderingsbehov, årsakTilOpprettelse, beskrivelse = melding.beskrivelse)
            )
            mottaDokumentService.markerSomBehandlet(sakId, behandling.id, referanse)
            prosesserBehandling.triggProsesserBehandling(
                sakId,
                behandling.id,
                vurderingsbehov = vurderingsbehov.map { it.type }
            )
        }
    }

    private fun knyttDokumentTilRiktigBehandling(
        opprettetBehandling: BehandlingService.OpprettetBehandling,
        sakId: SakId,
        referanse: InnsendingReferanse
    ) {
        when (opprettetBehandling) {
            is BehandlingService.Ordinær -> mottaDokumentService.oppdaterMedBehandlingId(
                sakId,
                opprettetBehandling.åpenBehandling.id,
                referanse
            )

            is BehandlingService.MåBehandlesAtomært -> mottaDokumentService.oppdaterMedBehandlingId(
                sakId,
                opprettetBehandling.nyBehandling.id,
                referanse
            )
        }
    }

    /**
     * Knytter klage og oppfølgingsbehandling direkte til behandlingen den opprettet, ikke via informasjonskrav.
     * Dette fordi det være flere åpne behandlinger av disse typene.
     * ManuellVurdering og NyÅrsakTilBehandling er knyttet eksplisitt til behandling og er ikke et informasjonskrav i flyten
     */
    private fun skalMarkereDokumentSomBehandlet(melding: Melding?): Boolean =
        melding is KabalHendelse || melding is Oppfølgingsoppgave || melding is ManuellRevurdering || melding is NyÅrsakTilBehandling || melding is TilbakekrevingHendelse

    private fun utledBeskrivelseForÅrsakTilOpprettelse(melding: Melding?): String? = when (melding) {
        is ManuellRevurderingV0 -> melding.beskrivelse
        is OmgjøringKlageRevurdering -> melding.beskrivelse
        is PdlHendelseV0 -> melding.beskrivelse
        is NyÅrsakTilBehandlingV0 -> melding.årsakerTilBehandling.joinToString(", ")
        is AnnetRelevantDokument -> melding.begrunnelse
        else -> null
    }

    private fun utledÅrsakTilOpprettelse(brevkategori: InnsendingType, melding: Melding?): ÅrsakTilOpprettelse {
        return when (brevkategori) {
            InnsendingType.SØKNAD -> ÅrsakTilOpprettelse.SØKNAD
            InnsendingType.AKTIVITETSKORT -> ÅrsakTilOpprettelse.AKTIVITETSMELDING
            InnsendingType.MELDEKORT -> ÅrsakTilOpprettelse.MELDEKORT
            InnsendingType.LEGEERKLÆRING -> ÅrsakTilOpprettelse.HELSEOPPLYSNINGER
            InnsendingType.LEGEERKLÆRING_AVVIST -> ÅrsakTilOpprettelse.HELSEOPPLYSNINGER
            InnsendingType.DIALOGMELDING -> ÅrsakTilOpprettelse.HELSEOPPLYSNINGER
            InnsendingType.KLAGE -> ÅrsakTilOpprettelse.KLAGE
            InnsendingType.ANNET_RELEVANT_DOKUMENT -> ÅrsakTilOpprettelse.ANNET_RELEVANT_DOKUMENT
            InnsendingType.MANUELL_REVURDERING -> ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE
            InnsendingType.NY_ÅRSAK_TIL_BEHANDLING -> ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE
            InnsendingType.KABAL_HENDELSE -> ÅrsakTilOpprettelse.SVAR_FRA_KLAGEINSTANS
            InnsendingType.OPPFØLGINGSOPPGAVE -> utledÅrsakTilOppfølgningsOppave(melding)
            InnsendingType.PDL_HENDELSE_DODSFALL_BRUKER -> ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA
            InnsendingType.PDL_HENDELSE_DODSFALL_BARN -> ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA
            InnsendingType.OMGJØRING_KLAGE_REVURDERING -> utledÅrsakEtterOmgjøringAvKlage(melding)
            InnsendingType.TILBAKEKREVING_HENDELSE -> ÅrsakTilOpprettelse.TILBAKEKREVING_HENDELSE
            InnsendingType.INSTITUSJONSOPPHOLD -> ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA
            InnsendingType.FAGSYSTEMINFO_BEHOV_HENDELSE -> ÅrsakTilOpprettelse.FAGSYSTEMINFO_BEHOV_HENDELSE
            InnsendingType.SYKEPENGE_VEDTAK_HENDELSE -> throw IllegalArgumentException("Sykepengevedtakhendelser skal trigge sjekk av informasjonskrav og ikke opprette en behandling direkte")
            InnsendingType.UFØRE_VEDTAK_HENDELSE -> ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA
        }
    }

    private fun utledÅrsakTilOppfølgningsOppave(melding: Melding?): ÅrsakTilOpprettelse {
        require(melding is OppfølgingsoppgaveV0) { "Melding must be of type OppfølgingsoppgaveV0" }
        val kode = melding.opprinnelse?.avklaringsbehovKode
        val stegType = kode?.let { finnStegType(it) }
        return when (stegType) {
            StegType.SAMORDNING_GRADERING -> ÅrsakTilOpprettelse.OPPFØLGINGSOPPGAVE_SAMORDNING_GRADERING
            else -> ÅrsakTilOpprettelse.OPPFØLGINGSOPPGAVE
        }
    }


    private fun finnStegType(avklaringsTypeKode: String): StegType {
        return Definisjon.forKode(avklaringsTypeKode).løsesISteg
    }

    private fun utledÅrsakEtterOmgjøringAvKlage(melding: Melding?): ÅrsakTilOpprettelse = when (melding) {
        is OmgjøringKlageRevurdering -> when (melding.kilde) {
            Omgjøringskilde.KLAGEINSTANS -> ÅrsakTilOpprettelse.OMGJØRING_ETTER_SVAR_FRA_KLAGEINSTANS
            Omgjøringskilde.KELVIN -> ÅrsakTilOpprettelse.OMGJØRING_ETTER_KLAGE
        }

        else -> error("Melding må være OmgjøringKlageRevurderingV0")
    }

    private fun utledVurderingsbehov(
        brevkategori: InnsendingType,
        melding: Melding?
    ): List<VurderingsbehovMedPeriode> {
        return when (brevkategori) {
            InnsendingType.SØKNAD -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD))
            InnsendingType.MANUELL_REVURDERING -> when (melding) {
                is ManuellRevurderingV0 -> melding.årsakerTilBehandling.map { VurderingsbehovMedPeriode(it.tilVurderingsbehov()) }
                else -> error("Melding må være ManuellRevurderingV0")
            }

            InnsendingType.OMGJØRING_KLAGE_REVURDERING -> when (melding) {
                is OmgjøringKlageRevurdering -> melding.vurderingsbehov.map { VurderingsbehovMedPeriode(it.tilVurderingsbehov()) }
                else -> error("Melding må være OmgjøringKlageRevurderingV0")
            }

            InnsendingType.MELDEKORT ->
                listOf(
                    VurderingsbehovMedPeriode(
                        type = Vurderingsbehov.MOTTATT_MELDEKORT,
                    )
                )

            InnsendingType.AKTIVITETSKORT -> listOf(
                VurderingsbehovMedPeriode(
                    type = Vurderingsbehov.MOTTATT_AKTIVITETSMELDING,
                )
            )

            InnsendingType.LEGEERKLÆRING_AVVIST -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_AVVIST_LEGEERKLÆRING))
            InnsendingType.LEGEERKLÆRING -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_LEGEERKLÆRING))
            InnsendingType.DIALOGMELDING -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_DIALOGMELDING))
            InnsendingType.ANNET_RELEVANT_DOKUMENT ->
                when (melding) {
                    is AnnetRelevantDokument -> melding.årsakerTilBehandling.map { VurderingsbehovMedPeriode(it.tilVurderingsbehov()) }
                    else -> error("Melding må være AnnetRelevantDokumentV0")
                }

            InnsendingType.KLAGE -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTATT_KLAGE))
            InnsendingType.NY_ÅRSAK_TIL_BEHANDLING ->
                when (melding) {
                    is NyÅrsakTilBehandlingV0 -> melding.årsakerTilBehandling.map { VurderingsbehovMedPeriode(it.tilVurderingsbehov()) }
                    else -> error("Melding må være NyÅrsakTilBehandlingV0")
                }

            InnsendingType.KABAL_HENDELSE -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_KABAL_HENDELSE))
            InnsendingType.OPPFØLGINGSOPPGAVE -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.OPPFØLGINGSOPPGAVE))
            InnsendingType.PDL_HENDELSE_DODSFALL_BRUKER -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.DØDSFALL_BRUKER))
            InnsendingType.PDL_HENDELSE_DODSFALL_BARN -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.DØDSFALL_BARN))
            InnsendingType.INSTITUSJONSOPPHOLD -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.INSTITUSJONSOPPHOLD))

            InnsendingType.TILBAKEKREVING_HENDELSE,
            InnsendingType.FAGSYSTEMINFO_BEHOV_HENDELSE,
            InnsendingType.SYKEPENGE_VEDTAK_HENDELSE,
            InnsendingType.UFØRE_VEDTAK_HENDELSE -> emptyList()
        }
    }

}
