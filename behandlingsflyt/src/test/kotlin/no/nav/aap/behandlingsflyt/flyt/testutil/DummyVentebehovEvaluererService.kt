package no.nav.aap.behandlingsflyt.flyt.testutil

import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.flyt.ventebehov.VentebehovEvaluererService
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst

class DummyVentebehovEvaluererService : VentebehovEvaluererService {
    override fun løsVentebehov(
        kontekst: FlytKontekst,
        avklaringsbehovene: Avklaringsbehovene
    ): List<Avklaringsbehov> {
        return emptyList()
    }
}