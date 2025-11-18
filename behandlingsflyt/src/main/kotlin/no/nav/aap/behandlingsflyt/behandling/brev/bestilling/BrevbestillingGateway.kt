package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.behandling.brev.BrevBehov
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.brev.kontrakt.MottakerDistStatus
import no.nav.aap.brev.kontrakt.MottakerDto
import no.nav.aap.brev.kontrakt.Signatur
import no.nav.aap.brev.kontrakt.SignaturGrunnlag
import no.nav.aap.brev.kontrakt.Vedlegg
import no.nav.aap.komponenter.gateway.Gateway
import java.io.InputStream

interface BrevbestillingGateway : Gateway {

    fun bestillBrevV2(
        saksnummer: Saksnummer,
        brukerIdent: Ident,
        behandlingReferanse: BehandlingReferanse,
        unikReferanse: String,
        brevBehov: BrevBehov,
        vedlegg: Vedlegg?,
        ferdigstillAutomatisk: Boolean,
    ): BrevbestillingReferanse

    fun ferdigstill(referanse: BrevbestillingReferanse, signaturer: List<SignaturGrunnlag>, mottakere: List<MottakerDto> =  emptyList()): Boolean

    fun hent(bestillingReferanse: BrevbestillingReferanse): BrevbestillingResponse

    fun oppdater(bestillingReferanse: BrevbestillingReferanse, brev: Brev)

    fun avbryt(bestillingReferanse: BrevbestillingReferanse)

    fun kanDistribuereBrev(mottakerIdentListe: List<String>, brevbestillingReferanse: BrevbestillingReferanse): List<MottakerDistStatus>

    fun hentSignaturForhåndsvisning(
        signaturer: List<SignaturGrunnlag>,
        brukerIdent: String,
        typeBrev: TypeBrev
    ): List<Signatur>

    fun forhåndsvis(bestillingReferanse: BrevbestillingReferanse, signaturer: List<SignaturGrunnlag>): InputStream
}
