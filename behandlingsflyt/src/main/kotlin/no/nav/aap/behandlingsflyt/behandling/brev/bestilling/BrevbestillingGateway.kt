package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.brev.kontrakt.Vedlegg

interface BrevbestillingGateway {
    fun bestillBrev(
        saksnummer: Saksnummer,
        behandlingReferanse: BehandlingReferanse,
        unikReferanse: String,
        typeBrev: TypeBrev,
        vedlegg: Vedlegg?
    ): BrevbestillingReferanse

    fun ferdigstill(referanse: BrevbestillingReferanse): Boolean

    fun hent(bestillingReferanse: BrevbestillingReferanse): BrevbestillingResponse

    fun oppdater(bestillingReferanse: BrevbestillingReferanse, brev: Brev)
}
