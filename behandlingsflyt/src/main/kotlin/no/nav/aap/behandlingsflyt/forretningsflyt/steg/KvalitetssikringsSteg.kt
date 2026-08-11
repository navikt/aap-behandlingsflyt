package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.VurderingEndretService
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.TilstrekkeligVurdert
import no.nav.aap.behandlingsflyt.flyt.steg.TilstrekkeligVurdertResultat
import no.nav.aap.behandlingsflyt.flyt.steg.TilstrekkeligVurdertResultat.Godkjent
import no.nav.aap.behandlingsflyt.flyt.steg.TilstrekkeligVurdertResultat.IkkeTilstrekkelig
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class KvalitetssikringsSteg(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val trekkKlageService: TrekkKlageService,
    private val avbrytRevurderingService: AvbrytRevurderingService,
    private val behandlingRepository: BehandlingRepository,
    private val behandlingService: BehandlingService,
    private val vurderingEndretService: VurderingEndretService,
    private val unleashGateway: UnleashGateway
) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(javaClass)

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        trekkKlageService = TrekkKlageService(repositoryProvider),
        avbrytRevurderingService = AvbrytRevurderingService(repositoryProvider),
        behandlingRepository = repositoryProvider.provide(),
        behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
        vurderingEndretService = VurderingEndretService(repositoryProvider),
        unleashGateway = gatewayProvider.provide()
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = Definisjon.KVALITETSSIKRING,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst, avklaringsbehovene) },
            erTilstrekkeligVurdert = {
                val resultat = erTilstrekkeligVurdert(
                    Input(
                        avklaringsbehovene,
                        kontekst.behandlingId,
                        vurderingEndretService,
                        unleashGateway
                    )
                ).also { if (it is IkkeTilstrekkelig) log.info("Ikke tilstrekkelig vurdert: ${it.melding}") }
                resultat.erTilstrekkelig()
            },
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
        return when (behandlingService.utledFaktiskBehandlingstype(behandling)) {
            TypeBehandling.Førstegangsbehandling,
            TypeBehandling.Klage -> {
                avklaringsbehovene.harAvklaringsbehovSomKreverKvalitetssikring()
            }

            else -> false
        }
    }

    companion object : FlytSteg, TilstrekkeligVurdert<Input> {
        override fun erTilstrekkeligVurdert(input: Input): TilstrekkeligVurdertResultat {
            val avklaringsbehovene = input.avklaringsbehovene
            val behandlingId = input.behandlingId
            val vurderingEndretService = input.vurderingEndretService
            val unleashGateway = input.unleashGateway
            if (unleashGateway.isEnabled(BehandlingsflytFeature.HoppOverKvalitetssikringVedIngenEndring)) {
                val forrigeKvalitetssikringTidspunkt =
                    avklaringsbehovene.hentBehovForDefinisjon(Definisjon.KVALITETSSIKRING)?.sistAvsluttetOrNull()
                        ?: return if (avklaringsbehovene.harAvklaringsbehovSomKreverKvalitetssikringMenIkkeErGodkjent())
                            IkkeTilstrekkelig("Det finnes avklaringsbehov som krever kvalitetssikring, men som ikke er godkjent.")
                        else Godkjent

                val harEndringPerAvklaringsbehov =
                    avklaringsbehovene.avklaringsbehovSomKreverKvalitetssikring().map { avklaringsbehov ->
                        vurderingEndretService.endretSidenTidspunkt(
                            behandlingId,
                            avklaringsbehov,
                            forrigeKvalitetssikringTidspunkt
                        )
                    }

                if (harEndringPerAvklaringsbehov.any { it == null }) {
                    return if (avklaringsbehovene.harAvklaringsbehovSomKreverKvalitetssikringMenIkkeErGodkjent())
                        IkkeTilstrekkelig("Det finnes avklaringsbehov som ikke er godkjent.")
                    else Godkjent
                }

                val harAvklaringsbehovSomIkkeErGodkjentFraFør =
                    avklaringsbehovene.avklaringsbehovSomKreverKvalitetssikring()
                        .any { !it.harBlittKvalitetssikretTidligere() }

                return when {
                    harEndringPerAvklaringsbehov.any { it == true } ->
                        IkkeTilstrekkelig("En eller flere vurderinger er endret siden forrige kvalitetssikring og må kvalitetssikres på nytt.")

                    harAvklaringsbehovSomIkkeErGodkjentFraFør ->
                        IkkeTilstrekkelig("Det finnes avklaringsbehov som ikke har blitt kvalitetssikret tidligere.")

                    else -> Godkjent
                }
            }
            return if (avklaringsbehovene.harAvklaringsbehovSomKreverKvalitetssikringMenIkkeErGodkjent())
                IkkeTilstrekkelig("Det finnes avklaringsbehov som krever kvalitetssikring, men som ikke er godkjent.")
            else Godkjent
        }

        override fun konstruer(
            repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return KvalitetssikringsSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.KVALITETSSIKRING
        }


    }

    data class Input(
        val avklaringsbehovene: Avklaringsbehovene,
        val behandlingId: BehandlingId,
        val vurderingEndretService: VurderingEndretService,
        val unleashGateway: UnleashGateway
    )
}

