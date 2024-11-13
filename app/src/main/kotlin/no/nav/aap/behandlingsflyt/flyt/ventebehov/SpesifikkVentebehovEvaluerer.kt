package no.nav.aap.behandlingsflyt.flyt.ventebehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

/**
 *
 */
sealed interface SpesifikkVentebehovEvaluerer {
    fun definisjon(): Definisjon
    fun ansesSomLøst(behanndlingId: BehandlingId, avklaringsbehov: Avklaringsbehov): Boolean
}