package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.brev.Forvaltningsmelding
import no.nav.aap.behandlingsflyt.behandling.brev.ForvaltningsmeldingKantIKant
import no.nav.aap.behandlingsflyt.behandling.brev.KlageMottatt
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdatoRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.kravVedOvergangFraArena
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class SendForvaltningsmeldingSteg(
    private val brevbestillingService: BrevbestillingService,
    private val behandlingRepository: BehandlingRepository,
    private val mottaDokumentService: MottaDokumentService,
    private val kravRepository: KravRepository,
    private val arenaMaksdatoRepository: ArenaMaksdatoRepository,
    private val unleashGateway: UnleashGateway,
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        when (kontekst.behandlingType) {
            TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering -> {
                val behandlingId = kontekst.behandlingId
                val behandling = behandlingRepository.hent(behandlingId)
                if (erBehandlingForMottattSøknad(kontekst.vurderingsbehovRelevanteForSteg) &&
                    !harAlleredeBestiltForvaltningsmeldingForBehandling(behandling)
                ) {
                    val brevBehov = if (erKantIKantMedArena(kontekst)) ForvaltningsmeldingKantIKant
                    else Forvaltningsmelding
                    brevbestillingService.bestill(
                        behandlingId = behandlingId,
                        brevBehov = brevBehov,
                        unikReferanse = "${behandling.referanse}-${brevBehov.typeBrev}",
                        ferdigstillAutomatisk = true
                    )
                }
            }
            TypeBehandling.Klage -> {
                val behandlingId = kontekst.behandlingId
                val behandling = behandlingRepository.hent(behandlingId)
                if (erBehandlingForMottattKlage(kontekst.vurderingsbehovRelevanteForSteg) &&
                    !harAlleredeBestiltKlageMottattForBehandling(behandling) &&
                    erMottattKlageFraJournalPost(behandlingId)
                    ) {
                    val brevBehov = KlageMottatt
                    brevbestillingService.bestill(
                        behandlingId = behandlingId,
                        brevBehov = brevBehov,
                        unikReferanse = "${behandling.referanse}-${brevBehov.typeBrev}",
                        ferdigstillAutomatisk = true
                    )
                }
            }

            else -> {}
        }

        return Fullført
    }

    private fun erMottattKlageFraJournalPost(behandlingId: BehandlingId): Boolean {
        val dokumenter = mottaDokumentService.hentMottattDokumenterAvType(behandlingId, InnsendingType.KLAGE)
        return dokumenter.any { it.referanse.type == InnsendingReferanse.Type.JOURNALPOST }
    }

    /**
     * Søknaden er kant i kant når et gjeldende krav dekker dagen etter maksdato i Arena. Gjelder bare
     * førstegangsbehandling: i en revurdering er Arena-maksdatoen kopiert fra forrige behandling, og
     * overgangen fra Arena hører til den første søknaden. Krever også
     * [BehandlingsflytFeature.HandtereMaksdatoForKantTilKant], som styrer om Arena-maksdatoen hentes.
     */
    private fun erKantIKantMedArena(kontekst: FlytKontekstMedPerioder): Boolean {
        if (kontekst.forrigeBehandlingId != null) return false
        if (!unleashGateway.isEnabled(BehandlingsflytFeature.NykantIkantForvaltningsmelding)) return false
        if (!unleashGateway.isEnabled(BehandlingsflytFeature.HandtereMaksdatoForKantTilKant)) return false

        val arenaMaksdato = arenaMaksdatoRepository.hentHvisEksisterer(kontekst.behandlingId)?.arenaMaksdato
            ?: return false
        val gjeldendeRelevanteKrav =
            kravRepository.hentHvisEksisterer(kontekst.behandlingId)?.gjeldendeRelevanteKrav().orEmpty()
        return kravVedOvergangFraArena(gjeldendeRelevanteKrav, arenaMaksdato) != null
    }

    private fun harAlleredeBestiltForvaltningsmeldingForBehandling(behandling: Behandling): Boolean {
        return brevbestillingService.hentBrevbestillinger(behandling.referanse)
            .any { it.typeBrev in FORVALTNINGSMELDINGER }
    }

    private fun harAlleredeBestiltKlageMottattForBehandling(behandling: Behandling): Boolean {
        return brevbestillingService.hentBrevbestillinger(behandling.referanse)
            .any { it.typeBrev == TypeBrev.KLAGE_MOTTATT}
    }


    private fun erBehandlingForMottattSøknad(årsakerTilBehandling: Set<Vurderingsbehov>): Boolean {
        return Vurderingsbehov.MOTTATT_SØKNAD in årsakerTilBehandling
    }

    private fun erBehandlingForMottattKlage(årsakerTilBehandling : Set<Vurderingsbehov>): Boolean {
        return Vurderingsbehov.MOTATT_KLAGE in årsakerTilBehandling
    }


    companion object : FlytSteg {
        private val FORVALTNINGSMELDINGER =
            setOf(TypeBrev.FORVALTNINGSMELDING, TypeBrev.FORVALTNINGSMELDING_KANT_I_KANT)

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return SendForvaltningsmeldingSteg(
                brevbestillingService = BrevbestillingService(repositoryProvider, gatewayProvider),
                behandlingRepository = repositoryProvider.provide(),
                mottaDokumentService = MottaDokumentService(repositoryProvider),
                kravRepository = repositoryProvider.provide(),
                arenaMaksdatoRepository = repositoryProvider.provide(),
                unleashGateway = gatewayProvider.provide(),
            )
        }

        override fun type(): StegType {
            return StegType.SEND_FORVALTNINGSMELDING
        }
    }
}
