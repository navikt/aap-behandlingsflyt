package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.Status
import java.util.UUID

interface BrevbestillingGateway {
    fun bestillBrev(
        behandlingReferanse: BehandlingReferanse,
        typeBrev: TypeBrev,
    ): UUID

    fun hentBestillingStatus(referanse: UUID): Status

    fun ferdigstill(referanse: UUID): Status
}
