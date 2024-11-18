package no.nav.aap.behandlingsflyt.flyt.ventebehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId

/**
 *
 */
sealed interface SpesifikkVentebehovEvaluerer {
    fun definisjon(): Definisjon
    fun ansesSomLøst(behandlingId: BehandlingId, avklaringsbehov: Avklaringsbehov, sakId: SakId): Boolean
}