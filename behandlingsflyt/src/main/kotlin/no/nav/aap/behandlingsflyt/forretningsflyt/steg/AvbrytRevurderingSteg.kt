package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.REVURDERING_AVBRUTT
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class AvbrytRevurderingSteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val avbrytRevurderingRepository: AvbrytRevurderingRepository
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (!erRelevant(kontekst)) {
            return Fullført
        }

        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val avbrytRevurderingGrunnlag =
            avbrytRevurderingRepository.hentHvisEksisterer(kontekst.behandlingId)

        if (avklaringsbehov.harIkkeBlittLøst(Definisjon.AVBRYT_REVURDERING)) {
            return FantAvklaringsbehov(Definisjon.AVBRYT_REVURDERING)
        }

        checkNotNull(avbrytRevurderingGrunnlag) {
            "Abryt revurdering har blitt satt som løst."
        }

        if (avbrytRevurderingGrunnlag.vurdering.årsak != null) {
            avklaringsbehov.avbrytÅpneAvklaringsbehov()
        }

        return Fullført
    }

    private fun erRelevant(kontekst: FlytKontekstMedPerioder): Boolean {
        return (kontekst.behandlingType == TypeBehandling.Revurdering)
                && (REVURDERING_AVBRUTT in kontekst.vurderingsbehovRelevanteForSteg)
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg {
            return AvbrytRevurderingSteg(
                avklaringsbehovRepository = repositoryProvider.provide(),
                avbrytRevurderingRepository = repositoryProvider.provide()
            )
        }

        override fun type(): StegType {
            return StegType.AVBRYT_REVURDERING
        }
    }

    private fun Avklaringsbehovene.harIkkeBlittLøst(definisjon: Definisjon): Boolean {
        return this.alle()
            .filter { it.definisjon == definisjon }
            .none{ it.status() == Status.AVSLUTTET }
    }

}