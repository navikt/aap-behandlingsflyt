package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

data class Brevbestilling(
    val id: Long,
    val behandlingId: BehandlingId,
    val typeBrev: TypeBrev,
    val referanse: BrevbestillingReferanse,
    val status: Status,
)