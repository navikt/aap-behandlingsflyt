package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import java.util.UUID

interface BrevbestillingGateway {
    fun bestillBrev(
        saksnummer: Saksnummer,
        behandlingReferanse: BehandlingReferanse,
        typeBrev: TypeBrev,
    ): UUID

    fun hentBestillingStatus(referanse: UUID): Status

    fun ferdigstill(referanse: UUID): Boolean
}
