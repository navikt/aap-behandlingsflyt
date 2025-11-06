package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class AvbrytRevurderingSteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val avbrytRevurderingRepository: AvbrytRevurderingRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val erTilstrekkeligVurdert = avbrytRevurderingRepository.hentHvisEksisterer(kontekst.behandlingId) != null

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId),
            definisjon = Definisjon.AVBRYT_REVURDERING,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst) },
            erTilstrekkeligVurdert = { erTilstrekkeligVurdert },
            tilbakestillGrunnlag = {},
            kontekst = kontekst
        )
        return Fullført
    }

    private fun vedtakBehøverVurdering(kontekst: FlytKontekstMedPerioder): Boolean {
        // Her gir det faktisk mening å sjekke på behandlingstype, siden man ikke kan avbryte revurdering
        // i en førstegangsbehandling.
        return (kontekst.behandlingType == TypeBehandling.Revurdering)
                && kontekst.vurderingsbehovRelevanteForSteg.isNotEmpty()
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return AvbrytRevurderingSteg(
                avklaringsbehovRepository = repositoryProvider.provide(),
                avbrytRevurderingRepository = repositoryProvider.provide(),
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider)
            )
        }

        override fun type(): StegType {
            return StegType.AVBRYT_REVURDERING
        }
    }
}