package no.nav.aap.behandlingsflyt.flate.behandling

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import java.util.*

data class BehandlingReferanse(@PathParam("referanse") val referanse: String) {
    fun ref(): UUID {
        return UUID.fromString(referanse)
    }
}
