package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravValidering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.NyttKrav
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
            "saksnumre"
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
                        VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING -> {
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
                        NyttKrav(
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

    private fun vurderHelautomatisk(kontekst: FlytKontekstMedPerioder) {
        val behandlingstype = behandlingService.utledFaktiskBehandlingstype(kontekst.behandlingId)
        
        val søknaderMottattIBehandling =
            mottattDokumentRepository.hentDokumenterAvType(kontekst.behandlingId, InnsendingType.SØKNAD)
                .sortedBy { it.mottattTidspunkt }
        val kravGrunnlag = kravRepository.hentHvisEksisterer(kontekst.behandlingId)
        
        // Dersom vi har overstyrt rettighetsperioden i denne behandlingen, 
        // beholder vi denne som nytt krav uavhengig av mottattidspunkt for søknad.
        val overstyrteKravIDenneBehandlingen = kravGrunnlag?.vurderinger
            ?.filterIsInstance<NyttKrav>()
            ?.filter { it.overstyrMuligRettFra != null }
            ?.filter { it.vurdertIBehandling == kontekst.behandlingId }
            .orEmpty().toSet()

        val vurderinger =
            søknaderMottattIBehandling.mapIndexed { index, søknad ->
                if (behandlingstype == TypeBehandling.Førstegangsbehandling
                    && overstyrteKravIDenneBehandlingen.isEmpty()
                    && index == 0
                ) {
                    nyttKrav(kontekst.behandlingId, søknad)
                } else {
                    tilleggsopplysning(kontekst.behandlingId, søknad)
                }
            }

        val vedtatteVurderinger =
            kontekst.forrigeBehandlingId?.let { kravRepository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()

        val resultat = vedtatteVurderinger + overstyrteKravIDenneBehandlingen + vurderinger
        if (resultat.isNotEmpty()) {
            kravRepository.lagre(kontekst.behandlingId, resultat)
        }
    }

    private fun nyttKrav(behandlingId: BehandlingId, søknad: MottattDokument): NyttKrav {
        return NyttKrav(
            referanse = Kravreferanse.ny(),
            journalpostId = søknad.referanse.asJournalpostId,
            vurdertAv = SYSTEMBRUKER,
            begrunnelse = "Automatisk vurdert",
            vurdertIBehandling = behandlingId,
            opprettet = Instant.now(),
            søknadsdato = Søknadsdato(
                søknad.mottattTidspunkt.toLocalDate(),
                SøknadsdatoÅrsak.SøknadMottatt
            ),
            overstyrMuligRettFra = null,
            muligRettFra = søknad.mottattTidspunkt.toLocalDate()
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
