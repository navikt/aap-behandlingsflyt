package no.nav.aap.behandlingsflyt.flyt.testutil

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.ventebehov.VentebehovEvaluererService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId

class DummyVentebehovEvaluererService : VentebehovEvaluererService {
    override fun ansesSomLøst(
        behandlingId: BehandlingId,
        avklaringsbehov: Avklaringsbehov,
        sakId: SakId
    ): Boolean {
        return true
    }
}