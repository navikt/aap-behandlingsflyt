package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.repository.Repository

interface BrevbestillingRepository : Repository{
    fun hent(behandlingId: BehandlingId, typeBrev: TypeBrev): Brevbestilling?
    fun hent(behandlingId: BehandlingId): List<Brevbestilling>
    fun lagre(
        behandlingId: BehandlingId,
        typeBrev: TypeBrev,
        bestillingReferanse: BrevbestillingReferanse,
        status: Status
    )

    fun oppdaterStatus(behandlingId: BehandlingId, referanse: BrevbestillingReferanse, status: Status)
}