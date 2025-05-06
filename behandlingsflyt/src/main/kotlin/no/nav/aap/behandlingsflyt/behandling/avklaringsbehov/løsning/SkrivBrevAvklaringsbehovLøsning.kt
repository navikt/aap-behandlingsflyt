package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import java.util.UUID

data class SkrivBrevAvklaringsbehovLøsning(val brevbestillingReferanse: UUID, val handling: Handling) {
    enum class Handling {
        FERDIGSTILL, AVBRYT
    }
}
