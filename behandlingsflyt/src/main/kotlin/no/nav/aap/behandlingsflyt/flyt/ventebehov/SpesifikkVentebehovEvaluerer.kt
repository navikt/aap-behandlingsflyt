package no.nav.aap.behandlingsflyt.flyt.ventebehov

import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId

/**
 *
 */
sealed interface SpesifikkVentebehovEvaluerer {
    fun definisjon(): Definisjon
    fun ansesSomLøst(behandlingId: BehandlingId, avklaringsbehov: Avklaringsbehov, sakId: SakId): Boolean
}