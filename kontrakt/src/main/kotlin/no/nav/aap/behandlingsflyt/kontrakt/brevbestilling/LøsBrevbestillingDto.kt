package no.nav.aap.behandlingsflyt.kontrakt.brevbestilling

import java.util.UUID

public data class LøsBrevbestillingDto(
    val behandlingReferanse: UUID,
    val bestillingReferanse: UUID,
    val status: BrevbestillingLøsningStatus
)
