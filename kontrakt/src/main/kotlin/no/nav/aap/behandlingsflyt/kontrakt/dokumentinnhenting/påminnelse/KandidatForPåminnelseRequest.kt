package no.nav.aap.behandlingsflyt.kontrakt.dokumentinnhenting.påminnelse

import java.time.LocalDate

public data class KandidatForPåminnelseRequest(
    val bestillingOpprettetDato: LocalDate,
)