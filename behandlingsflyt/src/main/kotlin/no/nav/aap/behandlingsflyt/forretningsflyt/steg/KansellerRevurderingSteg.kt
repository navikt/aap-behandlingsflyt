package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering.KansellerRevurderingRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class KansellerRevurderingSteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val kansellerRevurderingRepository: KansellerRevurderingRepository
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (erIkkeRelevant(kontekst)) {
            return Fullført
        }

        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val kansellerRevurderingGrunnlag =
            kansellerRevurderingRepository.hentKansellertRevurderingGrunnlag(kontekst.behandlingId)

        if (avklaringsbehov.harIkkeBlittLøst(Definisjon.KANSELLER_REVURDERING)) {
            return FantAvklaringsbehov(Definisjon.KANSELLER_REVURDERING)
        }

        checkNotNull(kansellerRevurderingGrunnlag) {
            "Kanseller revurdering har blitt satt som løst, men ingen grunnlag har blitt lagret på behandlingen."
        }

        if (kansellerRevurderingGrunnlag.vurdering.årsak != null) {
            avklaringsbehov.avbrytÅpneAvklaringsbehov()
        }

        return Fullført
    }

    private fun erIkkeRelevant(kontekst: FlytKontekstMedPerioder): Boolean {
        return Vurderingsbehov.REVURDERING_KANSELLERT !in kontekst.vurderingsbehovRelevanteForSteg
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg {
            return KansellerRevurderingSteg(
                avklaringsbehovRepository = repositoryProvider.provide(),
                kansellerRevurderingRepository = repositoryProvider.provide()
            )
        }

        override fun type(): StegType {
            return StegType.KANSELLER_REVURDERING
        }
    }

    private fun Avklaringsbehovene.harIkkeBlittLøst(definisjon: Definisjon): Boolean {
        return this.alle()
            .filter { it.definisjon == definisjon }
            .none{ it.status() == Status.AVSLUTTET }
    }

}