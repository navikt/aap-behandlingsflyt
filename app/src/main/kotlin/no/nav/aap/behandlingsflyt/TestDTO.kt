package no.nav.aap.behandlingsflyt

import com.fasterxml.jackson.annotation.JsonProperty
import com.papsign.ktor.openapigen.annotations.Response
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.pliktkort.Pliktkort
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
import org.jetbrains.annotations.NotNull
import java.time.LocalDate

@Response(statusCode = 202)
data class OpprettTestcaseDTO(
    @JsonProperty(value = "ident", required = true) val ident: String,
    @JsonProperty(value = "fødselsdato", required = true) val fødselsdato: LocalDate,
    @NotNull @JsonProperty(value = "yrkesskade", defaultValue = "false") val yrkesskade: Boolean,
    @NotNull @JsonProperty(value = "student", defaultValue = "false") val student: Boolean
)

data class OpprettYrkesskadeTestCase(
    @JsonProperty(value = "ident", required = true) val ident: String,
    @JsonProperty(value = "yrkesskade", required = true) val yrkesskadeDato: LocalDate
)


data class OpprettTestPersonDto(
    @JsonProperty(value = "fødselsdato", required = true) val fødselsdato: LocalDate,
    @NotNull @JsonProperty(value = "yrkesskade", defaultValue = "false") val yrkesskade: Boolean
)

data class OpprettTestPersonResponsDto(
    @JsonProperty(value = "ident", required = true) val ident: String
)

@Response(statusCode = 202)
data class PliktkortTestDTO(
    @JsonProperty(value = "ident", required = true) val ident: String,
    @JsonProperty(value = "pliktkort", required = true) @NotNull val pliktkort: Pliktkort
)