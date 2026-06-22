package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class KvalitetssikringsSteg(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val trekkKlageService: TrekkKlageService,
    private val avbrytRevurderingService: AvbrytRevurderingService,
    private val behandlingRepository: BehandlingRepository,
    private val behandlingService: BehandlingService,
    private val unleashGateway: UnleashGateway
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        trekkKlageService = TrekkKlageService(repositoryProvider),
        avbrytRevurderingService = AvbrytRevurderingService(repositoryProvider),
        behandlingRepository = repositoryProvider.provide(),
        behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
        unleashGateway = gatewayProvider.provide()
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = Definisjon.KVALITETSSIKRING,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst, avklaringsbehovene) },
            erTilstrekkeligVurdert = { erTilstrekkeligVurdert(avklaringsbehovene) },
            tilbakestillGrunnlag = {},
            kontekst
        )
        return Fullført
    }

    private fun vedtakBehøverVurdering(
        kontekst: FlytKontekstMedPerioder, avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())
            || trekkKlageService.klageErTrukket(
                kontekst.behandlingId
            )
            || avbrytRevurderingService.revurderingErAvbrutt(kontekst.behandlingId)

        ) {
            return false
        }

        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val behandlingstype =
            if (unleashGateway.isEnabled(BehandlingsflytFeature.RevurderingEtterAvslagSkalKvalitetssikres)) {
                behandlingService.utledFaktiskBehandlingstype(behandling)
            } else {
                kontekst.behandlingType
            }
        return when (behandlingstype) {
            TypeBehandling.Førstegangsbehandling,
            TypeBehandling.Klage -> {
                avklaringsbehovene.harAvklaringsbehovSomKreverKvalitetssikring()
            }

            else -> false
        }
    }

    private fun erTilstrekkeligVurdert(avklaringsbehovene: Avklaringsbehovene): Boolean {
        return !avklaringsbehovene.harAvklaringsbehovSomKreverKvalitetssikringMenIkkeErGodkjent()
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return KvalitetssikringsSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.KVALITETSSIKRING
        }
    }
}
