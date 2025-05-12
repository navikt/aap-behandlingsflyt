package no.nav.aap.behandlingsflyt

import com.fasterxml.jackson.annotation.JsonProperty
import com.papsign.ktor.openapigen.annotations.Response
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.TjenestePensjonRespons
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import org.jetbrains.annotations.NotNull
import java.time.LocalDate
import java.time.Year

data class Institusjoner(
    val fengsel: Boolean? = false,
    val sykehus: Boolean? = false,
)

@Response(statusCode = 202)
data class OpprettTestcaseDTO(
    @JsonProperty(value = "fødselsdato", required = true) val fødselsdato: LocalDate,
    @NotNull @JsonProperty(value = "yrkesskade", defaultValue = "false") val yrkesskade: Boolean,
    @JsonProperty(value = "uføre") val uføre: Int?,
    @NotNull @JsonProperty(value = "student", defaultValue = "false") val student: Boolean,
    @NotNull @JsonProperty(value = "barn") val barn: List<TestBarn> = emptyList(),
    @NotNull @JsonProperty(value = "medlemskap", defaultValue = "true") val medlemskap: Boolean,
    @JsonProperty(value = "inntekterPerAr") val inntekterPerAr: List<InntektPerÅrDto>? = null,
    @JsonProperty(value = "tjenestePensjon") val tjenestePensjon: TjenestePensjonRespons? = null,
    val institusjoner: Institusjoner = Institusjoner(),
    val sykepenger: List<TestPerson.Sykepenger> = emptyList(),
    val søknadsdato: LocalDate? = null,
)

data class TestBarn(
    @JsonProperty(value = "fodselsdato", required = true) val fodselsdato: LocalDate,
    val harRelasjon: Boolean = true
)

data class InntektPerÅrDto(val år: Int, val beløp: Beløp) {
    fun to() : InntektPerÅr {
        return InntektPerÅr(Year.of(år), beløp)
    }
}

