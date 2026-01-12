package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.brev.Forvaltningsmelding
import no.nav.aap.behandlingsflyt.behandling.brev.KlageMottatt
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
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
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class SendForvaltningsmeldingSteg(
    private val brevbestillingService: BrevbestillingService,
    private val behandlingRepository: BehandlingRepository,
    private val mottaDokumentService: MottaDokumentService
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        when (kontekst.behandlingType) {
            TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering -> {
                val behandlingId = kontekst.behandlingId
                val behandling = behandlingRepository.hent(behandlingId)
                if (erBehandlingForMottattSøknad(kontekst.vurderingsbehovRelevanteForSteg) &&
                    !harAlleredeBestiltForvaltningsmeldingForBehandling(behandling)
                ) {
                    val brevBehov = Forvaltningsmelding
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

    private fun harAlleredeBestiltForvaltningsmeldingForBehandling(behandling: Behandling): Boolean {
        return brevbestillingService.hentBrevbestillinger(behandling.referanse)
            .any { it.typeBrev == TypeBrev.FORVALTNINGSMELDING }
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
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return SendForvaltningsmeldingSteg(
                brevbestillingService = BrevbestillingService(repositoryProvider, gatewayProvider),
                behandlingRepository = repositoryProvider.provide(),
                mottaDokumentService = MottaDokumentService(repositoryProvider)
            )
        }

        override fun type(): StegType {
            return StegType.SEND_FORVALTNINGSMELDING
        }
    }
}
