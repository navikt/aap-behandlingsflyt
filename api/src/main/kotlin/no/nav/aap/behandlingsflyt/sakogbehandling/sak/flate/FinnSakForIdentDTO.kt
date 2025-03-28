package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.tilgang.plugin.kontrakt.Behandlingsreferanse
import java.util.*

data class FinnSakForIdentDTO(
    @JsonProperty(value = "ident", required = true) val ident: String,
    val behandlingsReferanse: UUID
) : Behandlingsreferanse {
    override fun hentAvklaringsbehovKode(): String? {
        return null
    }

    override fun behandlingsreferanseResolverInput(): String {
        return behandlingsReferanse.toString()
    }
}