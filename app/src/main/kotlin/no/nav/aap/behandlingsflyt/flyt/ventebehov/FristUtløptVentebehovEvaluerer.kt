package no.nav.aap.behandlingsflyt.flyt.ventebehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov

internal object FristUtløptVentebehovEvaluerer {

    fun ansesSomLøst(avklaringsbehov: Avklaringsbehov): Boolean {
        return avklaringsbehov.erÅpent() && avklaringsbehov.fristUtløpt()
    }
}