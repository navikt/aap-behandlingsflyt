package no.nav.aap.behandlingsflyt.flyt.ventebehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId

interface VentebehovEvaluererService {
    fun ansesSomLøst(behandlingId: BehandlingId, avklaringsbehov: Avklaringsbehov, sakId: SakId): Boolean
}