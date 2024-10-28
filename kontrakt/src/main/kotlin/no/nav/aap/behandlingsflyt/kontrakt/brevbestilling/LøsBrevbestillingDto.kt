package no.nav.aap.behandlingsflyt.kontrakt.brevbestilling

import java.util.UUID

data class LøsBrevbestillingDto(val referanse: UUID, val status: BrevbestillingLøsningStatus)
