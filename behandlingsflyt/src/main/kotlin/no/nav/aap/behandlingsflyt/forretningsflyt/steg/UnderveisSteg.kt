package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.underveis.UnderveisService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class UnderveisSteg(
    private val underveisService: UnderveisService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovRepository: AvklaringsbehovRepository
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        underveisService = UnderveisService(repositoryProvider, gatewayProvider),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        avklaringsbehovRepository = repositoryProvider.provide()
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (kontekst.vurderingType == VurderingType.FØRSTEGANGSBEHANDLING) {
            if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                return Fullført
            }
        }

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.REVURDERING -> {
                underveisService.vurder(kontekst.sakId, kontekst.behandlingId)

                val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
                if (kontekst.vurderingsbehovRelevanteForSteg.contains(Vurderingsbehov.REVURDER_MELDEPLIKT_RIMELIG_GRUNN) && avklaringsbehov.harIkkeForeslåttUttak()) {
                    return FantAvklaringsbehov(Definisjon.FORESLÅ_UTTAK)
                }
            }

            VurderingType.MELDEKORT, VurderingType.EFFEKTUER_AKTIVITETSPLIKT -> {
                underveisService.vurder(kontekst.sakId, kontekst.behandlingId)
            }

            VurderingType.IKKE_RELEVANT -> {
            }
        }
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg {
            return UnderveisSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.FASTSETT_UTTAK
        }
    }
}