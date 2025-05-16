package no.nav.aap.behandlingsflyt.flyt.testutil

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingGateway
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Faktagrunnlag
import no.nav.aap.brev.kontrakt.Signatur
import no.nav.aap.brev.kontrakt.SignaturGrunnlag
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.kontrakt.Vedlegg
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.LocalDateTime
import java.util.*

class FakeBrevbestillingGateway : BrevbestillingGateway {
    var brevbestillingResponse: BrevbestillingResponse? = null
    override fun bestillBrev(
        saksnummer: Saksnummer,
        brukerIdent: Ident,
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

    override fun bestillBrevV2(
        saksnummer: Saksnummer,
        brukerIdent: Ident,
        behandlingReferanse: BehandlingReferanse,
        unikReferanse: String,
        typeBrev: TypeBrev,
        vedlegg: Vedlegg?,
        faktagrunnlag: Set<Faktagrunnlag>,
        ferdigstillAutomatisk: Boolean
    ): BrevbestillingReferanse {
        return BrevbestillingReferanse(UUID.randomUUID())
    }

    override fun ferdigstill(referanse: BrevbestillingReferanse, signaturer: List<SignaturGrunnlag>): Boolean {
        brevbestillingResponse = brevbestillingResponse!!.copy(status = Status.FERDIGSTILT)
        return true
    }

    override fun avbryt(bestillingReferanse: BrevbestillingReferanse) {
        brevbestillingResponse = brevbestillingResponse!!.copy(status = Status.AVBRUTT)
    }

    override fun hent(bestillingReferanse: BrevbestillingReferanse): BrevbestillingResponse {
        return brevbestillingResponse!!
    }

    override fun oppdater(bestillingReferanse: BrevbestillingReferanse, brev: Brev) {
        brevbestillingResponse = brevbestillingResponse!!.copy(brev = brev)
    }

    override fun hentSignaturForhåndsvisning(
        signaturer: List<SignaturGrunnlag>,
        brukerIdent: String,
        typeBrev: TypeBrev
    ): List<Signatur> {
        return signaturer.map { Signatur(it.navIdent, "Nav Enhet") }
    }

    override fun forhåndsvis(
        bestillingReferanse: BrevbestillingReferanse,
        signaturer: List<SignaturGrunnlag>
    ): InputStream {
        return ByteArrayInputStream("test".toByteArray())
    }
}
