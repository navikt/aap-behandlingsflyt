package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider

class FritakMeldepliktSteg(
    private val meldepliktRepository: MeldepliktRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val unleashGateway: UnleashGateway
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        meldepliktRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        unleashGateway = gatewayProvider.provide()
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        // Todo: unleash toggle isDisabled --> return Fullført
//        if (unleashGateway.isDisabled()) {
//            return Fullført
//        }
        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = Definisjon.FRITAK_MELDEPLIKT,
            tvingerAvklaringsbehov = setOf(
                Vurderingsbehov.REVURDER_FRITAK_MELDEPLIKT
            ),
            nårVurderingErRelevant = { nårVurderingErRelevant(kontekst) },
            kontekst = kontekst,
            nårVurderingErGyldig = { nårVurderingErGyldig(kontekst) },
            tilbakestillGrunnlag = fun() {}
        )
        return Fullført
    }

    fun nårVurderingErRelevant(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        if (Vurderingsbehov.REVURDER_FRITAK_MELDEPLIKT in kontekst.vurderingsbehovRelevanteForSteg) {
            return Tidslinje(kontekst.rettighetsperiode, true)
        }
        return Tidslinje()
    }

    fun nårVurderingErGyldig(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        return Tidslinje(kontekst.rettighetsperiode, true)
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return FritakMeldepliktSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.FRITAK_MELDEPLIKT
        }
    }
}
