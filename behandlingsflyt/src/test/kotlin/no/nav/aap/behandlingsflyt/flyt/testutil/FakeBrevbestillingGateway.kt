package no.nav.aap.behandlingsflyt.flyt.testutil

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingGateway
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.kontrakt.Vedlegg
import java.time.LocalDateTime
import java.util.*

class FakeBrevbestillingGateway : BrevbestillingGateway {
    var brevbestillingResponse: BrevbestillingResponse? = null
    override fun bestillBrev(
        saksnummer: Saksnummer,
        behandlingReferanse: BehandlingReferanse,
        unikReferanse: String,
        typeBrev: TypeBrev,
        vedlegg: Vedlegg?
    ): BrevbestillingReferanse {
        return BrevbestillingReferanse(UUID.randomUUID())
            .also {
                brevbestillingResponse = BrevbestillingResponse(
                    referanse = it.brevbestillingReferanse,
                    brev = null,
                    opprettet = LocalDateTime.now(),
                    oppdatert = LocalDateTime.now(),
                    behandlingReferanse = behandlingReferanse.referanse,
                    brevtype = Brevtype.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT,
                    språk = Språk.NB,
                    status = Status.REGISTRERT
                )
            }
    }

    override fun ferdigstill(referanse: BrevbestillingReferanse): Boolean {
        brevbestillingResponse = brevbestillingResponse!!.copy(status = Status.FERDIGSTILT)
        return true
    }

    override fun hent(bestillingReferanse: BrevbestillingReferanse): BrevbestillingResponse {
        return brevbestillingResponse!!
    }

    override fun oppdater(bestillingReferanse: BrevbestillingReferanse, brev: Brev) {
        TODO("Not yet implemented")
    }
}