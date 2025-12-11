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
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
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
    private val unleashGateway: UnleashGateway,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        brevUtlederService = BrevUtlederService(repositoryProvider, gatewayProvider),
        brevbestillingService = BrevbestillingService(repositoryProvider, gatewayProvider),
        behandlingRepository = repositoryProvider.provide(),
        trekkKlageService = TrekkKlageService(repositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        avklaringsbehovRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide(),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val klageErTrukket = trekkKlageService.klageErTrukket(kontekst.behandlingId)
        val brevBehov = brevUtlederService.utledBehovForMeldingOmVedtak(kontekst.behandlingId)

        if (brevBehov != null && !klageErTrukket) {
            val eksisterendeBestilling = brevbestillingService.hentNyesteBestilling(kontekst.behandlingId, brevBehov.typeBrev)
            if (eksisterendeBestilling == null) {
                bestillBrev(kontekst, brevBehov)
            } else if (eksisterendeBestilling != null && eksisterendeBestilling.status.kanGjenopptas()) {
                gjenopptaBrevBestilling(kontekst)
            }
        }

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId),
            Definisjon.SKRIV_VEDTAKSBREV,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(klageErTrukket, brevBehov) },
            erTilstrekkeligVurdert = { erTilstrekkeligVurdert(kontekst.behandlingId, brevBehov) },
            tilbakestillGrunnlag = { tilbakestillGrunnlag(kontekst.behandlingId) },
            kontekst
        )

        return Fullført
    }

    private fun erTilstrekkeligVurdert(behandlingId: BehandlingId, brevBehov: BrevBehov?): Boolean {
        // gitt brevbehov=null er erTilstrekkeligVurdert input til oppdaterAvklaringsbehov() irrelevant og settes her til false
        if (brevBehov == null)  {
            return false
        }
        return brevbestillingService.erNyesteBestillingOmVedtakIEndeTilstand(behandlingId, brevBehov.typeBrev)
    }

    private fun gjenopptaBrevBestilling(kontekst: FlytKontekstMedPerioder) {
        brevbestillingService.gjenopptaVedtakBrevBestillinger(kontekst.behandlingId)
    }

    /**
     * BrevBestillinger i tilstand AVBRUTT, FORHÅNDSVISNING_KLAR (og SENDT) kan i teorien tilbakestilles. Den praktiske
     * begrensningen per i dag er at selve brevsteget ikke kan tilbakestilles (ingen fremtidige scenarior for dette foreløpig)
     */
    private fun tilbakestillGrunnlag(behandlingId: BehandlingId) {
        brevbestillingService.tilbakestillVedtakBrevBestillinger(behandlingId)
    }

    private fun vedtakBehøverVurdering(klageErTrukket: Boolean, brevBehov: BrevBehov?): Boolean {
        return !klageErTrukket && brevBehov != null
    }

    private fun bestillBrev(kontekst: FlytKontekstMedPerioder, brevBehov: BrevBehov) {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        log.info("Bestiller brev for sak ${kontekst.sakId}.")
        val unikReferanse = "${behandling.referanse}-${brevBehov.typeBrev}"
        brevbestillingService.bestill(
            behandlingId = kontekst.behandlingId,
            brevBehov = brevBehov,
            unikReferanse = unikReferanse,
            ferdigstillAutomatisk = false,
            brukApiV3 = brukApiV3(kontekst.behandlingId)
        )
    }

    private fun brukApiV3(behandlingId: BehandlingId): Boolean {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.FATTE_VEDTAK) ?: return false
        val endretAv = avklaringsbehov.endretAv()
        return unleashGateway.isEnabled(BehandlingsflytFeature.NyBrevbyggerV3, endretAv)
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