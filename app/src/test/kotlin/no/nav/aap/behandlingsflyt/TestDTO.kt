package no.nav.aap.behandlingsflyt

import com.fasterxml.jackson.annotation.JsonProperty
import com.papsign.ktor.openapigen.annotations.Response
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
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
    @param:JsonProperty(value = "fødselsdato", required = true) val fødselsdato: LocalDate,
    @param:NotNull @param:JsonProperty(value = "yrkesskade", defaultValue = "false") val yrkesskade: Boolean,
    @param:JsonProperty(value = "uføre") val uføre: Int?,
    @param:NotNull @param:JsonProperty(value = "student", defaultValue = "false") val student: Boolean,
    @param:NotNull @param:JsonProperty(value = "barn") val barn: List<TestBarn> = emptyList(),
    @param:NotNull @param:JsonProperty(value = "medlemskap", defaultValue = "true") val medlemskap: Boolean,
    @param:JsonProperty(value = "inntekterPerAr") val inntekterPerAr: List<InntektPerÅrDto>? = null,
    @param:JsonProperty(value = "tjenestePensjon") val tjenestePensjon: Boolean? = null,
    val institusjoner: Institusjoner = Institusjoner(),
    val sykepenger: List<TestPerson.Sykepenger> = emptyList(),
    val søknadsdato: LocalDate? = null,
)

data class TestBarn(
    @param:JsonProperty(value = "fodselsdato", required = true) val fodselsdato: LocalDate,
    val harRelasjon: Boolean = true
)

data class InntektPerÅrDto(val år: Int, val beløp: Beløp) {
    fun to(): InntektPerÅr {
        return InntektPerÅr(Year.of(år), beløp)
    }
}

