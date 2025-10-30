@file:JvmName("MeldingOmVedtakBrevStegKt")

package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.brev.BrevBehov
import no.nav.aap.behandlingsflyt.behandling.brev.BrevUtlederService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("BrevSteg")

class MeldingOmVedtakBrevSteg(
    private val brevUtlederService: BrevUtlederService,
    private val brevbestillingService: BrevbestillingService,
    private val behandlingRepository: BehandlingRepository,
    private val trekkKlageService: TrekkKlageService,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        brevUtlederService = BrevUtlederService(repositoryProvider, gatewayProvider),
        brevbestillingService = BrevbestillingService(repositoryProvider, gatewayProvider),
        behandlingRepository = repositoryProvider.provide(),
        trekkKlageService = TrekkKlageService(repositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        avklaringsbehovRepository = repositoryProvider.provide()
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (trekkKlageService.klageErTrukket(kontekst.behandlingId)) {
            return Fullført
        }
        val brevBehov = brevUtlederService.utledBehovForMeldingOmVedtak(kontekst.behandlingId)
        if (brevBehov == null) {
            return Fullført
        }
        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId),
            Definisjon.SKRIV_VEDTAKSBREV,
            vedtakBehøverVurdering = { true },
            erTilstrekkeligVurdert = { brevbestillingService.harBestillingOmVedtak(kontekst.behandlingId) },
            tilbakestillGrunnlag = {},
            kontekst
        )
        if (!brevbestillingService.harBestillingOmVedtak(kontekst.behandlingId)) {
            bestillBrev(kontekst, brevBehov)
        }
        return Fullført
    }

    private fun bestillBrev(kontekst: FlytKontekstMedPerioder, brevBehov: BrevBehov) {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        log.info("Bestiller brev for sak ${kontekst.sakId}.")
        val unikReferanse = "${behandling.referanse}-${brevBehov.typeBrev}"
        brevbestillingService.bestillV2(
            behandlingId = kontekst.behandlingId,
            brevBehov = brevBehov,
            unikReferanse = unikReferanse,
            ferdigstillAutomatisk = false,
        )
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg {
            return MeldingOmVedtakBrevSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.BREV
        }
    }
}