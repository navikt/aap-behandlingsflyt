package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository

interface BrevbestillingRepository : Repository {
    fun hent(sakId: SakId, typeBrev: TypeBrev): List<Brevbestilling>
    fun hent(behandlingId: BehandlingId): List<Brevbestilling>
    fun hent(brevbestillingReferanse: BrevbestillingReferanse): Brevbestilling?
    fun lagre(
        behandlingId: BehandlingId,
        typeBrev: TypeBrev,
        bestillingReferanse: BrevbestillingReferanse,
        status: Status
    )

    fun oppdaterStatus(behandlingId: BehandlingId, referanse: BrevbestillingReferanse, status: Status)
    fun hentBehandlingsreferanseForBestilling(referanse: BrevbestillingReferanse): BehandlingReferanse
}