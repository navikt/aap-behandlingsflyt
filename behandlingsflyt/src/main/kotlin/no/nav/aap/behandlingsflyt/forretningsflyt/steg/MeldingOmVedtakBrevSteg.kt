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
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
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

    /**
     * TODO: AAP-1676 : vurder å gjøre teoretisk tilbakestillGrunnlag() fullt funksjonell
     *
     * En behandling kan kun ha ett vedtaksbrev og ny brevbestilling per i dag er ikke mulig hvis det finnes et avbrutt
     * vedtaksbrev. Da må avbrutt vedtaksbrev isteden endre status fra AVBRUTT til FORHÅNDSVISNING_KLAR slik at det
     * kan sparkes igang igjen med nytt kall til fremtidig API-endepunkt brevbestilling/gjenoppta-bestilling i aap-brev.
     * Først da vil brevet kunne behandles videre igjen. Hvis brev-steg plutselig blir mulig å tilbakestille med nåværende
     * tilbakestillGrunnlag() logikk, så vil utfør() feile når ny runde i BrevSteg.utfør() trigges da vedtaksbrev med
     * status AVBRUTT må kunne gjenopptas og settes tilbake til FORHÅNDSVISNING_KLAR i aap-behandlingsflyt og aap-brev
     * må motta API-kall /gjenoppta-bestilling slik at brevet igjen får status UNDER_ARBEID i aap-brev
     */
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val klageErTrukket = trekkKlageService.klageErTrukket(kontekst.behandlingId)
        val brevBehov = brevUtlederService.utledBehovForMeldingOmVedtak(kontekst.behandlingId)
        val harBestillingOmVedtakBrev = brevbestillingService.harBestillingOmVedtak(kontekst.behandlingId)
        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId),
            Definisjon.SKRIV_VEDTAKSBREV,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(klageErTrukket, brevBehov) },
            erTilstrekkeligVurdert = { brevbestillingService.erAlleBestillingerOmVedtakIEndeTilstand(kontekst.behandlingId) },
            tilbakestillGrunnlag = { tilbakestillGrunnlag(kontekst.behandlingId) },
            kontekst
        )
        if (brevBehov != null && !klageErTrukket && !harBestillingOmVedtakBrev) {
            bestillBrev(kontekst, brevBehov)
        }
        return Fullført
    }

    /**
     * Brevbestillinger i endeTilstand (FULLFØRT, AVBRUTT) kan per i dag ikke tilbakestilles i aap-behandlingsflyt.
     * Hvis dette endres i fremtiden må også tilbakestillGrunnlag() logikken her tilpasses.
     *
     * BrevBestillinger i tilstand FORHÅNDSVISNING_KLAR og SENDT kan i teorien tilbakestilles. Den praktiske
     * begrensningen per i dag er at selve brev steget ikke kan tilbakestilles (ingen fremtidige scenarior for dette foreløpig)
     * og i tillegg at funsjonalitet for å gjenoppta-brevbestilling via aap-brev må implementeres (AAP-1676)
     */
    private fun tilbakestillGrunnlag(behandlingId: BehandlingId) {
        if (!brevbestillingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)) {
            val brevBestillingerOmVedtakSomKanTilbakestilles =
                brevbestillingService.hentTilbakestillbareBestillingerOmVedtak(behandlingId)
            for (brevBestilling in brevBestillingerOmVedtakSomKanTilbakestilles) {
                brevbestillingService.avbryt(brevBestilling.behandlingId, brevBestilling.referanse)
            }
        }
    }

    private fun vedtakBehøverVurdering(klageErTrukket: Boolean, brevBehov: BrevBehov?): Boolean {
        return !klageErTrukket && brevBehov != null
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