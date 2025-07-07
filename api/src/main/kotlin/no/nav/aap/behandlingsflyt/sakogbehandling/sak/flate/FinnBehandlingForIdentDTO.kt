package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class FinnBehandlingForIdentDTO(
    @param:JsonProperty(value = "ident", required = true) val ident: String,
    @param:JsonProperty(value = "mottattTidspunkt", required = true) val mottattTidspunkt: LocalDate
)