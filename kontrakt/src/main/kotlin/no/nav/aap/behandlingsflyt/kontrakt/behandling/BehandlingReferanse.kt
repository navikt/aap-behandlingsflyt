package no.nav.aap.behandlingsflyt.kontrakt.behandling

import com.fasterxml.jackson.annotation.JsonValue
import java.util.*

data class BehandlingReferanse(@JsonValue val referanse: UUID) {
    override fun toString() = referanse.toString()
}
