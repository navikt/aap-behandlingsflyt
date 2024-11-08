package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.verdityper.sakogbehandling.BehandlingId

data class Brevbestilling(
    val id: Long,
    val behandlingId: BehandlingId,
    val typeBrev: TypeBrev,
    val referanse: BrevbestillingReferanse,
    val status: Status,
)