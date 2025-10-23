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
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.REVURDERING_AVBRUTT
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class AvbrytRevurderingSteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val avbrytRevurderingRepository: AvbrytRevurderingRepository
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.AVBRYT_REVURDERING,
            vedtakBehøverVurdering = {
                kontekst.behandlingType == TypeBehandling.Revurdering
                        && REVURDERING_AVBRUTT in kontekst.vurderingsbehovRelevanteForSteg
            },
            erTilstrekkeligVurdert = { true },
            tilbakestillGrunnlag = {
                val avbrytRevurderingGrunnlag =
                    checkNotNull(avbrytRevurderingRepository.hentHvisEksisterer(kontekst.behandlingId)) {
                        "Avbryt revurdering har blitt satt som løst."
                    }

                if (avbrytRevurderingGrunnlag.vurdering.årsak != null) {
                    avklaringsbehovene.avbrytÅpneAvklaringsbehov()
                }
            },
            kontekst = kontekst
        )

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return AvbrytRevurderingSteg(
                avklaringsbehovRepository = repositoryProvider.provide(),
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
                avbrytRevurderingRepository = repositoryProvider.provide()
            )
        }

        override fun type(): StegType {
            return StegType.AVBRYT_REVURDERING
        }
    }
}