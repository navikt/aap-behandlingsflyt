package no.nav.aap.behandlingsflyt.kontrakt.brev

import java.util.UUID

data class BrevbestillingStatusDto(val referanse: UUID, val status: Status)