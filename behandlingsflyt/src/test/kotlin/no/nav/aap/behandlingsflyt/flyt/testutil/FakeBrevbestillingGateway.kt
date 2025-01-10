package no.nav.aap.behandlingsflyt.flyt.testutil

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingGateway
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.brev.kontrakt.Vedlegg
import java.util.*

class FakeBrevbestillingGateway : BrevbestillingGateway {
    var brevbestillingReferanse: BrevbestillingReferanse? = null
    override fun bestillBrev(
        saksnummer: Saksnummer,
        behandlingReferanse: BehandlingReferanse,
        unikReferanse: String,
        typeBrev: TypeBrev,
        vedlegg: Vedlegg?
    ): BrevbestillingReferanse {
        return BrevbestillingReferanse(UUID.randomUUID())
            .also {
                brevbestillingReferanse = it
            }
    }

    override fun ferdigstill(referanse: BrevbestillingReferanse): Boolean {
        TODO("Not yet implemented")
    }

    override fun hent(bestillingReferanse: BrevbestillingReferanse): BrevbestillingResponse {
        TODO("Not yet implemented")
    }

    override fun oppdater(bestillingReferanse: BrevbestillingReferanse, brev: Brev) {
        TODO("Not yet implemented")
    }
}