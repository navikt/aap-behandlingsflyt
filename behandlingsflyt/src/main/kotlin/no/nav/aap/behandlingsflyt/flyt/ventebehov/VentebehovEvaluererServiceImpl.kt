package no.nav.aap.behandlingsflyt.flyt.ventebehov

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.komponenter.repository.RepositoryProvider

class VentebehovEvaluererServiceImpl(private val repositoryProvider: RepositoryProvider) : VentebehovEvaluererService {

    private val fristEvaluerer = FristUtløptVentebehovEvaluerer

    private val evaluerere = SpesifikkVentebehovEvaluerer::class.sealedSubclasses.map { evaluerer ->
        evaluerer.constructors
            .find { it.parameters.singleOrNull()?.type?.classifier == RepositoryProvider::class }!!
            .call(repositoryProvider)
    }

    private fun ansesSomLøst(kontekst: FlytKontekst, avklaringsbehov: Avklaringsbehov): Boolean {
        if (fristEvaluerer.ansesSomLøst(avklaringsbehov)) {
            return true
        }

        return evaluerere.filter { it.definisjon() == avklaringsbehov.definisjon }
            .any { it.ansesSomLøst(kontekst.behandlingId, avklaringsbehov, kontekst.sakId) }
    }

    override fun løsVentebehov(
        kontekst: FlytKontekst,
        avklaringsbehovene: Avklaringsbehovene
    ): List<Avklaringsbehov> {
        val ventebehovSomErLøst = avklaringsbehovene.hentÅpneVentebehov()
            .filter { behov -> ansesSomLøst(kontekst, behov) }

        for (ventebehov in ventebehovSomErLøst) {
            avklaringsbehovene.løsAvklaringsbehov(
                definisjon = ventebehov.definisjon,
                begrunnelse = "Ventebehov løst.",
                endretAv = SYSTEMBRUKER.ident
            )
        }

        return ventebehovSomErLøst
    }
}
