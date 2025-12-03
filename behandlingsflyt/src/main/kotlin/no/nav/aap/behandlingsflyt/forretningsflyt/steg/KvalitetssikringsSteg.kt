package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.TilbakeføresFraKvalitetsikrer
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class KvalitetssikringsSteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val trekkKlageService: TrekkKlageService,
    private val unleashGateway: UnleashGateway,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        trekkKlageService = TrekkKlageService(repositoryProvider),
        unleashGateway = gatewayProvider.provide(),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (unleashGateway.isEnabled(BehandlingsflytFeature.KvalitetssikringsSteg)) {
            return utførNy(kontekst)
        }
        return utførGammel(kontekst)
    }

    fun utførNy(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.KVALITETSSIKRING,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst, avklaringsbehovene) },
            erTilstrekkeligVurdert = { erTilstrekkeligVurdert(avklaringsbehovene) },
            tilbakestillGrunnlag = {},
            kontekst
        )

        if (avklaringsbehovene.skalTilbakeføresEtterKvalitetssikring()) {
            return TilbakeføresFraKvalitetsikrer
        }

        return Fullført
    }

    fun utførGammel(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (kontekst.behandlingType !in listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Klage)) {
            return Fullført
        }

        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type()) || trekkKlageService.klageErTrukket(
                kontekst.behandlingId
            )
        ) {
            avklaringsbehov.avbrytForSteg(type())
            return Fullført
        }



        if (avklaringsbehov.skalTilbakeføresEtterKvalitetssikring()) {
            return TilbakeføresFraKvalitetsikrer
        }
        if (avklaringsbehov.harHattAvklaringsbehovSomKreverKvalitetssikring()) {
            return FantAvklaringsbehov(Definisjon.KVALITETSSIKRING)
        }

        return Fullført
    }

    private fun vedtakBehøverVurdering(
        kontekst: FlytKontekstMedPerioder, avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        if (tidligereVurderinger.girIngenBehandlingsgrunnlag(
                kontekst, type()
            ) || trekkKlageService.klageErTrukket(
                kontekst.behandlingId
            )
        ) {
            return false
        }

        return when (kontekst.behandlingType) {
            TypeBehandling.Førstegangsbehandling,
            TypeBehandling.Klage -> {
                return avklaringsbehovene.harAvklaringsbehovSomKreverKvalitetssikring()
            }

            else -> false
        }
    }

    private fun erTilstrekkeligVurdert(avklaringsbehovene: Avklaringsbehovene): Boolean {
        if (avklaringsbehovene.alle()
            .filter { it.kreverKvalitetssikring() }
            .any { it.status() == Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER || it.status() == Status.SENDT_TILBAKE_FRA_BESLUTTER }
        ) {
            return false
        }

        val aktuelleAvklaringsbehovForKvalitetssikring = avklaringsbehovene.alle()
            .filter { it.kreverKvalitetssikring() }
            .filter { it.status() != Status.AVBRUTT }

        /**
         * Aldri tidligere blitt kvalitetssikret
         */
        if (!aktuelleAvklaringsbehovForKvalitetssikring.any { it.erKvalitetssikretTidligere() }) {
            return false
        }

        /**
         * Når kvalitetssikrer godkjenner, men beslutter underkjenner så skal steget sendes på nytt til kvalitetssikring
         */
        aktuelleAvklaringsbehovForKvalitetssikring.forEach { avklaringsbehov ->
            val sistReturnertFraBeslutter = avklaringsbehov.historikk.lastOrNull { historikk -> historikk.status == Status.SENDT_TILBAKE_FRA_BESLUTTER }
            val sistKvalitetssikret = avklaringsbehov.historikk.lastOrNull { historikk -> historikk.status == Status.KVALITETSSIKRET }

            if (sistReturnertFraBeslutter != null && sistKvalitetssikret != null) {
                return sistKvalitetssikret > sistReturnertFraBeslutter
            }
        }

        return true
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
