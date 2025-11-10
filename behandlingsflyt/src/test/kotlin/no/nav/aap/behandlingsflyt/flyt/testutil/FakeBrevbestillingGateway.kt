package no.nav.aap.behandlingsflyt.flyt.testutil

import no.nav.aap.behandlingsflyt.behandling.brev.BrevBehov
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingGateway
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.MottakerDistStatus
import no.nav.aap.brev.kontrakt.MottakerDto
import no.nav.aap.brev.kontrakt.Signatur
import no.nav.aap.brev.kontrakt.SignaturGrunnlag
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.kontrakt.Vedlegg
import no.nav.aap.komponenter.gateway.Factory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.LocalDateTime
import java.util.*

class FakeBrevbestillingGateway : BrevbestillingGateway {
    var brevbestillingResponse: BrevbestillingResponse? = null

    override fun bestillBrevV2(
        saksnummer: Saksnummer,
        brukerIdent: Ident,
        behandlingReferanse: BehandlingReferanse,
        unikReferanse: String,
        brevBehov: BrevBehov,
        vedlegg: Vedlegg?,
        ferdigstillAutomatisk: Boolean
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
                    status = Status.UNDER_ARBEID
                )
            }
    }

    override fun ferdigstill(referanse: BrevbestillingReferanse, signaturer: List<SignaturGrunnlag>, mottakere: List<MottakerDto>): Boolean {
        brevbestillingResponse = brevbestillingResponse!!.copy(status = Status.FERDIGSTILT)
        return true
    }

    override fun avbryt(bestillingReferanse: BrevbestillingReferanse) {
        brevbestillingResponse = brevbestillingResponse!!.copy(status = Status.AVBRUTT)
    }

    override fun kanDistribuereBrev(
        saksnummer: String,
        brukerIdent: String,
        mottakerIdentListe: List<String>
    ): List<MottakerDistStatus> {
        val brevKanDistribueres = MottakerDistStatus(brukerIdent, true)
        val brevKanIkkeDistribueres = MottakerDistStatus("1234", false)
        return listOf(brevKanDistribueres, brevKanIkkeDistribueres)
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

    companion object : Factory<BrevbestillingGateway> {
        override fun konstruer(): BrevbestillingGateway = FakeBrevbestillingGateway()
    }
}
