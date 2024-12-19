package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.LocalDateTime

data class FinnBehandlingForIdentDTO(
    @JsonProperty(value = "ident", required = true) val ident: String,
    @JsonProperty(value = "mottattTidspunkt", required = true) val mottattTidspunkt: LocalDate
)