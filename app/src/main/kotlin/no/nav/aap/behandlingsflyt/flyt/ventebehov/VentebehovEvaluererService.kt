package no.nav.aap.behandlingsflyt.flyt.ventebehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import kotlin.reflect.full.primaryConstructor

class VentebehovEvaluererService(private val connection: DBConnection) {

    private val fristEvaluerer = FristUtløptVentebehovEvaluerer

    private val evaluerere = SpesifikkVentebehovEvaluerer::class.sealedSubclasses.map { evaluerer ->
        evaluerer.primaryConstructor!!.call(connection)
    }

    fun ansesSomLøst(behandlingId: BehandlingId, avklaringsbehov: Avklaringsbehov): Boolean {
        if (fristEvaluerer.ansesSomLøst(avklaringsbehov)) {
            return true
        }

        return evaluerere.filter { it.definisjon() == avklaringsbehov.definisjon }
            .any { it.ansesSomLøst(behandlingId, avklaringsbehov) }
    }
}