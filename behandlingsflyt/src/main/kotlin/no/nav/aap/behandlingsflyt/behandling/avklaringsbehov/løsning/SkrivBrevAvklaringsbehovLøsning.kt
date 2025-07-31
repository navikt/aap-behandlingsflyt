package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import no.nav.aap.brev.kontrakt.MottakerDto
import java.util.UUID

data class SkrivBrevAvklaringsbehovLøsning(val brevbestillingReferanse: UUID, val handling: Handling, val mottakere: List<MottakerDto> = emptyList()) {
    enum class Handling {
        FERDIGSTILL, AVBRYT
    }
}
