package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.brev.BrevbestillingReferanse
import no.nav.aap.behandling.BehandlingId
import java.time.LocalDateTime

data class Brevbestilling(
    val id: Long,
    val behandlingId: BehandlingId,
    val typeBrev: TypeBrev,
    val referanse: BrevbestillingReferanse,
    val status: Status,
    val opprettet: LocalDateTime,
)