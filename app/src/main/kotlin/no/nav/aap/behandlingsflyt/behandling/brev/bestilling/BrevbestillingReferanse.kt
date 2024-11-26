package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import com.fasterxml.jackson.annotation.JsonValue
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import java.util.UUID

data class BrevbestillingReferanse(@JsonValue @PathParam("brevbestillingReferanse") val brevbestillingReferanse: UUID) {
    override fun toString(): String {
        return brevbestillingReferanse.toString()
    }
}
