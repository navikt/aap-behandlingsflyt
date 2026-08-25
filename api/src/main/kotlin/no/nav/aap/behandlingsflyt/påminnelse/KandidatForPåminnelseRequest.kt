package no.nav.aap.behandlingsflyt.påminnelse

import java.time.LocalDate

data class KandidatForPåminnelseRequest(
    val bestillingOpprettetDato: LocalDate,
)