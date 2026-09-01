package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravValidering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.OverstyrMuligRettFra
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Tilleggsopplysning
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Instant
import kotlin.collections.filterIsInstance

class KravSteg(
    private val unleashGateway: UnleashGateway,
    private val kravRepository: KravRepository,
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val sakRepository: SakRepository,
    private val behandlingService: BehandlingService
) : BehandlingSteg {

    /**
     * Backfill:
     * Alle saker må backfilles til at første søknad er første krav
     * Mer komplisert: Første søknad etter "rent avslag" bør være nytt krav
     * For "resten": Alle søknader er ikke et eget "krav". Opphør/stans og gjeninntreden kan trolig holdes unna for backfill
     */
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.KravSteg)
        ) {
            return Fullført
        }

        val erManuellVurderingAktivertForSak = unleashGateway.erPåskruddForSak(
            BehandlingsflytFeature.KravManuellVurdering,
            "saksnummer"
        ) { sakRepository.hent(kontekst.sakId).saksnummer }

        if (unleashGateway.isEnabled(BehandlingsflytFeature.KravAutomatiskVurdering)
            && !erManuellVurderingAktivertForSak
        ) {
            vurderHelautomatisk(kontekst)
        }

        if (erManuellVurderingAktivertForSak) {
            when (kontekst.behandlingType) {
                TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering -> {
                    when (kontekst.vurderingType) {
                        VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING, VurderingType.MIGERING_FRA_ARENA -> {
                            vurderAutomatiskHvisMulig(kontekst)

                            avklaringsbehovService.oppdaterAvklaringsbehov(
                                definisjon = Definisjon.VURDER_KRAV,
                                vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst) },
                                erTilstrekkeligVurdert = { erTilstrekkeligVurdert(kontekst) },
                                tilbakestillGrunnlag = { },
                                kontekst = kontekst
                            )
                        }

                        VurderingType.OVERGANG_UFORE_STANS,
                        VurderingType.MELDEKORT,
                        VurderingType.UTVID_VEDTAKSLENGDE,
                        VurderingType.MIGRER_RETTIGHETSPERIODE,
                        VurderingType.AUTOMATISK_BREV,
                        VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
                        VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
                        VurderingType.G_REGULERING,
                        VurderingType.IKKE_RELEVANT -> {
                        }
                    }
                }

                else -> {}
            }
        }

        return Fullført
    }

    private fun erTilstrekkeligVurdert(kontekst: FlytKontekstMedPerioder): Boolean {
        val søknaderIBehandling =
            mottattDokumentRepository.hentDokumenterAvType(kontekst.behandlingId, InnsendingType.SØKNAD)
        val kravVurderinger = kravRepository.hentHvisEksisterer(kontekst.behandlingId)?.vurderinger.orEmpty()

        return KravValidering.erKravVurderingTilstrekkeligVurdert(søknaderIBehandling, kravVurderinger)
    }

    private fun vedtakBehøverVurdering(kontekst: FlytKontekstMedPerioder): Boolean {
        val søknaderIBehandling =
            mottattDokumentRepository.hentDokumenterAvType(kontekst.behandlingId, InnsendingType.SØKNAD)
        val harSøknadIBehandling = søknaderIBehandling.isNotEmpty()
        val kravVurderinger = kravRepository.hentHvisEksisterer(kontekst.behandlingId)?.vurderinger.orEmpty()

        val erAlleSøknaderIBehandlingAutomatiskVurdert =
            søknaderIBehandling.all { søknad -> kravVurderinger.any { it.journalpostId == søknad.referanse.asJournalpostId && it.erAutomatiskVurdert() } }

        return (harSøknadIBehandling && !erAlleSøknaderIBehandlingAutomatiskVurdert) || kontekst.vurderingsbehovRelevanteForSteg.contains(
            Vurderingsbehov.VURDER_KRAV
        )
    }

    private fun vurderAutomatiskHvisMulig(kontekst: FlytKontekstMedPerioder) {
        if (erFørstegangsbehandlingUtenEksisterendeKrav(kontekst)) {
            val søknaderMottattIBehandling =
                mottattDokumentRepository.hentDokumenterAvType(kontekst.behandlingId, InnsendingType.SØKNAD)

            if (søknaderMottattIBehandling.size == 1) {
                val søknad = søknaderMottattIBehandling.first()
                kravRepository.lagre(
                    kontekst.behandlingId, vurderinger = setOf(
                        RelevantKrav(
                            referanse = Kravreferanse.ny(),
                            journalpostId = søknad.referanse.asJournalpostId,
                            vurdertAv = SYSTEMBRUKER,
                            begrunnelse = "Automatisk vurdert",
                            vurdertIBehandling = kontekst.behandlingId,
                            opprettet = Instant.now(),
                            søknadsdato = Søknadsdato(
                                søknad.mottattTidspunkt.toLocalDate(),
                                SøknadsdatoÅrsak.SøknadMottatt
                            ),
                            overstyrMuligRettFra = null,
                            muligRettFra = søknad.mottattTidspunkt.toLocalDate()
                        )
                    )
                )
            }
        }
    }

    // Denne antar kun ett relevant krav. Når vi skal støtte flere relevante krav må vi løfte avklaringsbehov
    private fun vurderHelautomatisk(kontekst: FlytKontekstMedPerioder) {
        val kravGrunnlag = kravRepository.hentHvisEksisterer(kontekst.behandlingId)

        // Early return: Forrige behandling er ikke migrert – BackfillKrav håndterer den.
        if (kravGrunnlag == null && kontekst.forrigeBehandlingId != null) return

        val søknaderMottattIBehandling =
            mottattDokumentRepository.hentDokumenterAvType(kontekst.behandlingId, InnsendingType.SØKNAD)

        // Legeerklæring kan i noen tilfeller være første dokument på en første behandlingen.
        val legeerklæringerMottattIBehandling = if (kontekst.forrigeBehandlingId == null) {
            mottattDokumentRepository.hentDokumenterAvType(kontekst.behandlingId, InnsendingType.LEGEERKLÆRING)
        } else {
            emptyList()
        }

        val alleDokumenter = (søknaderMottattIBehandling + legeerklæringerMottattIBehandling)
            .sortedBy { it.mottattTidspunkt }

        // Dersom saksbehandler har overstyrt muligRettFra i denne behandlingen, bevarer vi overstyringen
        // (verdien) men lar KravSteg re-vurdere hvilken søknad som er RelevantKrav.
        val overstyringFraDenneBehandlingen = kravGrunnlag
            ?.gjeldendeRelevanteKrav()
            ?.firstOrNull { it.vurdertIBehandling == kontekst.behandlingId }
            ?.overstyrMuligRettFra

        val vedtatteVurderinger =
            kontekst.forrigeBehandlingId?.let { kravRepository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()

        val gjeldendeKravFraForrige = KravGrunnlag(vedtatteVurderinger.toSet())
            .gjeldendeRelevanteKrav()
            .minByOrNull { it.muligRettFra }

        // Dersom eldste nye dokument er eldre enn det vedtatte kravets søknadsdato, overtar det som nytt RelevantKrav.
        val dokumentSomOvertarSomKrav = if (gjeldendeKravFraForrige != null) {
            alleDokumenter.firstOrNull {
                it.mottattTidspunkt.toLocalDate().isBefore(gjeldendeKravFraForrige.søknadsdato.dato)
            }
        } else null

        val overstyringFraVedtattKrav = dokumentSomOvertarSomKrav
            ?.let { gjeldendeKravFraForrige?.overstyrMuligRettFra }

        val gjeldendeOverstyring = overstyringFraDenneBehandlingen ?: overstyringFraVedtattKrav

        val vurderinger = alleDokumenter.mapIndexedNotNull { index, dokument ->
            val erNyttKrav = dokumentSomOvertarSomKrav?.referanse == dokument.referanse
                || (gjeldendeKravFraForrige == null && index == 0)
            when {
                erNyttKrav -> nyttKrav(kontekst.behandlingId, dokument, gjeldendeOverstyring)
                dokument.type == InnsendingType.SØKNAD -> tilleggsopplysning(kontekst.behandlingId, dokument)
                else -> null // Legeerklæring som ikke er eldste dokument – ingen separat vurdering
            }
        }

        // Nedgrader det vedtatte kravet til Tilleggsopplysning ved å legge til ny rad med samme referanse.
        // gjeldendeVurderinger() plukker maxBy { opprettet } per referanse, så den nye Tilleggsopplysning vinner.
        val nedgradertVedtattKrav = if (dokumentSomOvertarSomKrav != null && gjeldendeKravFraForrige != null) {
            setOf(
                Tilleggsopplysning(
                    referanse = gjeldendeKravFraForrige.referanse,
                    journalpostId = gjeldendeKravFraForrige.journalpostId,
                    vurdertAv = SYSTEMBRUKER,
                    begrunnelse = "Automatisk vurdert",
                    vurdertIBehandling = kontekst.behandlingId,
                    opprettet = Instant.now(),
                )
            )
        } else emptySet()

        val resultat = vedtatteVurderinger + vurderinger + nedgradertVedtattKrav
        if (resultat.isNotEmpty()) {
            kravRepository.lagre(kontekst.behandlingId, resultat)
        }
    }

    private fun nyttKrav(
        behandlingId: BehandlingId,
        dokument: MottattDokument,
        overstyrMuligRettFra: OverstyrMuligRettFra? = null,
    ): RelevantKrav {
        return RelevantKrav(
            referanse = Kravreferanse.ny(),
            journalpostId = dokument.referanse.asJournalpostId,
            vurdertAv = SYSTEMBRUKER,
            begrunnelse = "Automatisk vurdert",
            vurdertIBehandling = behandlingId,
            opprettet = Instant.now(),
            søknadsdato = Søknadsdato(
                dokument.mottattTidspunkt.toLocalDate(),
                SøknadsdatoÅrsak.SøknadMottatt
            ),
            overstyrMuligRettFra = overstyrMuligRettFra,
            muligRettFra = listOfNotNull(
                dokument.mottattTidspunkt.toLocalDate(),
                overstyrMuligRettFra?.dato,
            ).min(),
        )
    }

    private fun tilleggsopplysning(behandlingId: BehandlingId, søknad: MottattDokument): Tilleggsopplysning {
        return Tilleggsopplysning(
            referanse = Kravreferanse.ny(),
            journalpostId = søknad.referanse.asJournalpostId,
            vurdertAv = SYSTEMBRUKER,
            begrunnelse = "Automatisk vurdert",
            vurdertIBehandling = behandlingId,
            opprettet = Instant.now(),
        )
    }

    private fun erFørstegangsbehandlingUtenEksisterendeKrav(kontekst: FlytKontekstMedPerioder): Boolean {
        val erFørstegangsbehandling =
            Vurderingsbehov.MOTTATT_SØKNAD in kontekst.vurderingsbehovRelevanteForSteg && kontekst.behandlingType == TypeBehandling.Førstegangsbehandling
        return erFørstegangsbehandling
                && kravRepository.hentHvisEksisterer(kontekst.behandlingId)?.vurderinger.isNullOrEmpty()
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return KravSteg(
                unleashGateway = gatewayProvider.provide(),
                kravRepository = repositoryProvider.provide(),
                mottattDokumentRepository = repositoryProvider.provide(),
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider),
                sakRepository = repositoryProvider.provide(),
                behandlingService = BehandlingService(repositoryProvider, gatewayProvider)
            )
        }

        override fun type(): StegType {
            return StegType.KRAV
        }
    }
}
