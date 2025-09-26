package no.nav.aap.behandlingsflyt.forretningsflyt.steg.aktivitetsplikt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderAktivitetsplikt11_9Steg(
    private val unleashGateway: UnleashGateway,
    private val avklaringsbehovRepository: AvklaringsbehovRepository
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.Aktivitetsplikt11_9)) {
            throw IllegalStateException(
                "Steg ${StegType.VURDER_AKTIVITETSPLIKT_11_9} er deaktivert i unleash, kan ikke utføre steg."
            )
        }
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        if (!avklaringsbehovene.erVurdertTidligereIBehandlingen(Definisjon.VURDER_BRUDD_11_9)) {
            return FantAvklaringsbehov(Definisjon.VURDER_BRUDD_11_9)
        }
        
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderAktivitetsplikt11_9Steg(
                gatewayProvider.provide(),
                repositoryProvider.provide()
            )
        }

        override fun type(): StegType {
            return StegType.VURDER_AKTIVITETSPLIKT_11_9
        }
    }
}