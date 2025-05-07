package no.nav.aap.behandlingsflyt.flyt.ventebehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.repository.RepositoryProvider

class VentebehovEvaluererServiceImpl(private val repositoryProvider: RepositoryProvider) : VentebehovEvaluererService {

    private val fristEvaluerer = FristUtløptVentebehovEvaluerer

    private val evaluerere = SpesifikkVentebehovEvaluerer::class.sealedSubclasses.map { evaluerer ->
        evaluerer.constructors
            .find { it.parameters.singleOrNull()?.type?.classifier == RepositoryProvider::class }!!
            .call(repositoryProvider)
    }

    override fun ansesSomLøst(behandlingId: BehandlingId, avklaringsbehov: Avklaringsbehov, sakId: SakId): Boolean {
        if (fristEvaluerer.ansesSomLøst(avklaringsbehov)) {
            return true
        }

        return evaluerere.filter { it.definisjon() == avklaringsbehov.definisjon }
            .any { it.ansesSomLøst(behandlingId, avklaringsbehov, sakId) }
    }
}