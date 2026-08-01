package no.nav.aap.brev

import com.fasterxml.jackson.annotation.JsonValue
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import java.util.UUID

data class BrevbestillingReferanse(@JsonValue @param:PathParam("brevbestillingReferanse") val brevbestillingReferanse: UUID) {
    override fun toString(): String {
        return brevbestillingReferanse.toString()
    }
}