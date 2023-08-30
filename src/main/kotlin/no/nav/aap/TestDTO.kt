package no.nav.aap

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class OpprettTestcaseDTO(
    @JsonProperty(value = "ident", required = true) val ident: String,
    @JsonProperty(value = "fødselsdato", required = true) val fødselsdato: LocalDate,
    @JsonProperty(value = "yrkesskade", defaultValue = "false") val yrkesskade: Boolean
)