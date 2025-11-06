package no.nav.aap.behandlingsflyt.forretningsflyt.steg.aktivitetsplikt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9Repository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderAktivitetsplikt11_9Steg(
    private val unleashGateway: UnleashGateway,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val aktivitetsplikt11_9Repository: Aktivitetsplikt11_9Repository,
    private val avklaringsbehovService: AvklaringsbehovService,
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.Aktivitetsplikt11_9)) {
            throw IllegalStateException(
                "Steg ${StegType.VURDER_AKTIVITETSPLIKT_11_9} er deaktivert i unleash, kan ikke utføre steg."
            )
        }
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        avklaringsbehovService.oppdaterAvklaringsbehov(
            kontekst = kontekst,
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.VURDER_BRUDD_11_9,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst) },
            erTilstrekkeligVurdert = { true },
            tilbakestillGrunnlag = { tilbakestillGrunnlag(kontekst) })

        return Fullført
    }

    private fun tilbakestillGrunnlag(kontekst: FlytKontekstMedPerioder) {
        val tidligereVurderinger =
            kontekst.forrigeBehandlingId?.let { aktivitetsplikt11_9Repository.hentHvisEksisterer(it) }?.vurderinger
                ?: emptySet()

        val alleVurderinger =
            aktivitetsplikt11_9Repository.hentHvisEksisterer(kontekst.behandlingId)?.vurderinger ?: emptySet()

        if (tidligereVurderinger != alleVurderinger) {
            aktivitetsplikt11_9Repository.lagre(kontekst.behandlingId, tidligereVurderinger)
        }
    }

    private fun vedtakBehøverVurdering(kontekst: FlytKontekstMedPerioder): Boolean {
        return Vurderingsbehov.AKTIVITETSPLIKT_11_9 in kontekst.vurderingsbehovRelevanteForSteg
    }


    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderAktivitetsplikt11_9Steg(
                gatewayProvider.provide(),
                repositoryProvider.provide(),
                aktivitetsplikt11_9Repository = repositoryProvider.provide(),
                AvklaringsbehovService(repositoryProvider),
            )
        }

        override fun type(): StegType {
            return StegType.VURDER_AKTIVITETSPLIKT_11_9
        }
    }
}