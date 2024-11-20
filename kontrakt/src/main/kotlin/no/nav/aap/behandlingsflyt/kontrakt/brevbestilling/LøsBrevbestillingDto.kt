package no.nav.aap.behandlingsflyt.kontrakt.brevbestilling

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.tilgang.plugin.kontrakt.Behandlingsreferanse
import java.util.UUID

public data class LøsBrevbestillingDto(
    val behandlingReferanse: UUID,
    val bestillingReferanse: UUID,
    val status: BrevbestillingLøsningStatus
) : Behandlingsreferanse {
    override fun hentAvklaringsbehovKode(): String? {
        return Definisjon.BESTILL_BREV.kode.toString()
    }

    override fun hentBehandlingsreferanse(): String {
        return behandlingReferanse.toString()
    }
}
