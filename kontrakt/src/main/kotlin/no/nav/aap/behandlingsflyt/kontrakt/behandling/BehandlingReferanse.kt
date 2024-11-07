package no.nav.aap.behandlingsflyt.kontrakt.behandling

import com.fasterxml.jackson.annotation.JsonValue
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import java.util.*

public data class BehandlingReferanse(@JsonValue @PathParam("referanse") val referanse: UUID = UUID.randomUUID()) {
    override fun toString(): String {
        return referanse.toString()
    }
}
