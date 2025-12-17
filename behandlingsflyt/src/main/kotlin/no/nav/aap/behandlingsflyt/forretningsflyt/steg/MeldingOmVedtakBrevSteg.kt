@file:JvmName("MeldingOmVedtakBrevStegKt")

package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.brev.BrevBehov
import no.nav.aap.behandlingsflyt.behandling.brev.BrevUtlederService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
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
import java.util.*

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
            val eksisterendeBestilling = brevbestillingService.hentNyesteAktiveBestilling(kontekst.behandlingId, brevBehov.typeBrev)
            if (eksisterendeBestilling == null) {
                bestillBrev(kontekst, brevBehov)
            }
            // TODO : rydd vekk eksisterende vedtakbrev bestillinger hvis brevbehov er endret - ANNULLER+slett ?
        }

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId),
            Definisjon.SKRIV_VEDTAKSBREV,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(klageErTrukket, brevBehov) },
            erTilstrekkeligVurdert = { erTilstrekkeligVurdert(kontekst.behandlingId) },
            tilbakestillGrunnlag = { tilbakestillGrunnlag(kontekst.behandlingId) },
            kontekst
        )

        return Fullført
    }

    /**
     * ereTilstrekkeligVurdert vet nødvendigvis ikke hvilket konkrete vedtakbrev som ble vurdert av saksbehandler, da brevBehov er null.
     * Men det bestilles kun vedtakbrev fra BrevSteg og kun ett vedtakbrev vil være relevant til en hver tid.
     */
    private fun erTilstrekkeligVurdert(behandlingId: BehandlingId): Boolean {
        return brevbestillingService.erNyesteBestillingerOmVedtakIEndeTilstand(behandlingId)
    }

    /**
     * TilbakestillGrunnlag vet nødvendigvis ikke hvilket konkrete vedtakbrev som skal tilbakestilles, da brevBehov kan være null.
     * Men det bestilles kun vedtakbrev fra BrevSteg og kun ett vedtakbrev vil være relevant til en hver tid.
     *
     * Dermed kan alle vedtakbrev som har tilstand FORHÅNDSVISNING_KLAR tilbakestilles
     * (alltid forventet å være ett vedtakbrev per behandling)
     *
     * I dagens behandlingsflyt vil tilbakestill i praksis aldri forekomme da brevsteget i seg selv ikke kan
     * tilbakestilles (ingen fremtidige scenarior for dette foreløpig)
     */
    private fun tilbakestillGrunnlag(behandlingId: BehandlingId) {
        brevbestillingService.tilbakestillAlleAktiveBestillingerOmVedtakbrev(behandlingId)
    }

    private fun vedtakBehøverVurdering(klageErTrukket: Boolean, brevBehov: BrevBehov?): Boolean {
        return !klageErTrukket && brevBehov != null
    }

    private fun bestillBrev(kontekst: FlytKontekstMedPerioder, brevBehov: BrevBehov): UUID {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        log.info("Bestiller brev for sak ${kontekst.sakId}.")
        val unikReferanse = "${behandling.referanse}-${brevBehov.typeBrev}"
        val bestillingReferanse = brevbestillingService.bestill(
            behandlingId = kontekst.behandlingId,
            brevBehov = brevBehov,
            unikReferanse = unikReferanse,
            ferdigstillAutomatisk = false,
            brukApiV3 = brukApiV3(kontekst.behandlingId)
        )
        return bestillingReferanse
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