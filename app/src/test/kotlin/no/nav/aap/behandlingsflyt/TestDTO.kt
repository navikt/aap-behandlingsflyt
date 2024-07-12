package no.nav.aap.behandlingsflyt

import com.fasterxml.jackson.annotation.JsonProperty
import com.papsign.ktor.openapigen.annotations.Response
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.pliktkort.Pliktkort
import org.jetbrains.annotations.NotNull
import java.time.LocalDate

data class Institusjoner(
    val fengsel: Boolean? = false,
    val sykehus: Boolean? = false,
)

@Response(statusCode = 202)
data class OpprettTestcaseDTO(
    @JsonProperty(value = "fødselsdato", required = true) val fødselsdato: LocalDate,
    @NotNull @JsonProperty(value = "yrkesskade", defaultValue = "false") val yrkesskade: Boolean,
    @NotNull @JsonProperty(value = "student", defaultValue = "false") val student: Boolean,
    @NotNull @JsonProperty(value = "barn") val barn: List<TestBarn> = emptyList(),
    val institusjoner: Institusjoner = Institusjoner()
)

data class TestBarn(@JsonProperty(value = "fodselsdato", required = true) val fodselsdato: LocalDate)

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