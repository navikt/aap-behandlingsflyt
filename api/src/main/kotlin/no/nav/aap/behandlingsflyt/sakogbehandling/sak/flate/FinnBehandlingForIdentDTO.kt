package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.tilgang.plugin.kontrakt.Behandlingsreferanse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class FinnBehandlingForIdentDTO(
    @JsonProperty(value = "ident", required = true) val ident: String,
    @JsonProperty(value = "mottattTidspunkt", required = true) val mottattTidspunkt: LocalDate,
    val behandlingsReferanse: UUID
) : Behandlingsreferanse {
    override fun hentAvklaringsbehovKode(): String? {
        return Definisjon.BESTILL_LEGEERKLÆRING.kode.toString()
    }

    override fun behandlingsreferanseResolverInput(): String {
        return behandlingsReferanse.toString()
    }
}