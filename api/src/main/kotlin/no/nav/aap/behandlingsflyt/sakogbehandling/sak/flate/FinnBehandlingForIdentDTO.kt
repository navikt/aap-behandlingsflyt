package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.tilgang.plugin.kontrakt.Behandlingsreferanse
import java.time.LocalDate
import java.util.*

data class FinnBehandlingForIdentDTO(
    @JsonProperty(value = "ident", required = true) val ident: String,
    @JsonProperty(value = "mottattTidspunkt", required = true) val mottattTidspunkt: LocalDate,
    val behandlingsReferanse: UUID
) : Behandlingsreferanse {
    override fun hentAvklaringsbehovKode(): String? {
        return null
    }

    override fun behandlingsreferanseResolverInput(): String {
        return behandlingsReferanse.toString()
    }
}