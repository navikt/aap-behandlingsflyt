package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import java.util.UUID

interface BrevbestillingGateway {
    fun bestillBrev(
        behandlingReferanse: BehandlingReferanse,
        typeBrev: TypeBrev,
    ): UUID
}
