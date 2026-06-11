package no.nav.aap.behandlingsflyt

import com.fasterxml.jackson.annotation.JsonProperty
import com.papsign.ktor.openapigen.annotations.Response
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerKilde
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerYtelseType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.TiltakspengerKilde
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.TiltakspengerYtelseType
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AndreUtbetalingerDto
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
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

data class TestYrkesskadeDto(
    val kilde: Kilde = Kilde.REGISTER,         // "SØKNAD" eller "REGISTER"
    val harYrkesskade: Boolean = false,     // brukes når kilde=SØKNAD
    val skadeart: String? = null,           // brukes når kilde=REGISTER
    val diagnose: String? = null,           // brukes når kilde=REGISTER
    val skadebeskrivelse: String? = null,   // brukes når kilde=REGISTER
    val skadedato: LocalDate? = null,
    val saksreferanse: String = "1234",
    val vedtaksdato: LocalDate? = null,
)

enum class SamordningType {
    SYKEPENGER,
    DAGPENGER,
    TILTAKSPENGER,
}

data class SamordningDto(
    val type: SamordningType,
    val sykepengerGrad: Int? = null,
    val dagpengerYtelseType: String? = null,
    val dagpengerKilde: String? = null,
    val tiltakspengerYtelseType: String? = null,
    val tiltakspengerKilde: String? = null,
    val periode: PeriodeDto,
)

data class PeriodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
)

data class KravVurderingTestDto(
    val kravType: KravType,
    val søknadsdato: LocalDate? = LocalDate.now().minusMonths(3),
    val kravdato: LocalDate? = LocalDate.now().minusMonths(3),
    val muligRettFra: LocalDate? = LocalDate.now().minusMonths(1),
)

data class LeggTilKravVurderingDTO(
    val kravVurderinger: List<KravVurderingTestDto> = listOf(KravVurderingTestDto(KravType.NYTT_KRAV_AAP))
)

@Response(statusCode = 202)
data class OpprettTestcaseDTO(
    @param:JsonProperty(value = "fødselsdato", required = true) val fødselsdato: LocalDate,
    @param:NotNull @param:JsonProperty(value = "yrkesskader") val yrkesskader: List<TestYrkesskadeDto> = emptyList(),
    @param:JsonProperty(value = "uføre") val uføre: Int?,
    @param:JsonProperty(value = "uføretidspunkt") val uføreTidspunkt: LocalDate?,
    @param:JsonProperty(value = "uføregradTom") val uføregradTom: LocalDate?,
    @param:JsonProperty(value = "uføreSøknadDato") val uføreSøknadDato: LocalDate?,
    @param:NotNull @param:JsonProperty(value = "student", defaultValue = "false") val student: Boolean,
    @param:NotNull @param:JsonProperty(value = "barn") val barn: List<TestBarn> = emptyList(),
    @param:NotNull @param:JsonProperty(value = "medlemskap", defaultValue = "true") val medlemskap: Boolean,
    val fastlege: TestFastlege? = null,
    @param:JsonProperty(value = "inntekterPerAr") val inntekterPerAr: List<InntektPerÅrDto>? = null,
    @param:JsonProperty(value = "tjenestePensjon") val tjenestePensjon: Boolean? = null,
    val institusjoner: Institusjoner = Institusjoner(),
    val samordning: List<SamordningDto> = emptyList(),
    val kravVurderinger: List<KravVurderingTestDto> = emptyList(),
    val søknadsdato: LocalDate? = null,
    val andreUtbetalinger: AndreUtbetalingerDto? = null,
    val steg: StegType? = null,
    val harNedsattArbeidsevne: Boolean = true,
    val erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean = true,
) {
    val harYrkesskade: Boolean
        get() = harYrkesskadeFraSøknad || yrkesskader.any { it.kilde == Kilde.REGISTER }

    val harYrkesskadeFraSøknad: Boolean
        get() = yrkesskader.any { it.kilde == Kilde.SØKNAD && it.harYrkesskade }

    val sykepenger: List<TestPerson.Sykepenger>
        get() = samordning
            .filter { it.type == SamordningType.SYKEPENGER }
            .map { TestPerson.Sykepenger(grad = it.sykepengerGrad ?: 100, periode = Periode(it.periode.fom, it.periode.tom)) }

    val dagpenger: List<TestPerson.Dagpenger>
        get() = samordning
            .filter { it.type == SamordningType.DAGPENGER }
            .map {
                TestPerson.Dagpenger(
                    periode = Periode(it.periode.fom, it.periode.tom),
                    kilde = it.dagpengerKilde?.let { k -> DagpengerKilde.valueOf(k) } ?: DagpengerKilde.DP_SAK,
                    dagpengerYtelseType = it.dagpengerYtelseType?.let { t -> DagpengerYtelseType.valueOf(t) }
                        ?: DagpengerYtelseType.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                )
            }

    val tiltakspenger: List<TestPerson.Tiltakspenger>
        get() = samordning
            .filter { it.type == SamordningType.TILTAKSPENGER }
            .map {
                TestPerson.Tiltakspenger(
                    periode = Periode(it.periode.fom, it.periode.tom),
                    kilde = it.tiltakspengerKilde?.let { k -> TiltakspengerKilde.valueOf(k) } ?: TiltakspengerKilde.TPSAK,
                    ytelseType = it.tiltakspengerYtelseType?.let { t -> TiltakspengerYtelseType.valueOf(t) }
                        ?: TiltakspengerYtelseType.TILTAKSPENGER,
                )
            }
}

enum class Kilde {
    SØKNAD,
    REGISTER
}

data class LeggTilInstitusjonsoppholdDTO(
    @param:JsonProperty(value = "opphold", required = true)
    val opphold: List<InstitusjonsoppholdItemDTO>
)

data class InstitusjonsoppholdItemDTO(
    @param:JsonProperty(value = "institusjonstype", required = true) val institusjonstype: Institusjonstype,
    @param:JsonProperty(value = "oppholdstype", required = true) val oppholdstype: Oppholdstype,
    @param:JsonProperty(value = "oppholdFom", required = true) val oppholdFom: LocalDate,
    @param:JsonProperty(value = "oppholdTom", required = true) val oppholdTom: LocalDate,
)

data class LeggTilYrkesskadeDTO(
    val yrkesskader: List<TestYrkesskadeDto> = listOf(TestYrkesskadeDto()),
)

data class TestBarn(
    @param:JsonProperty(value = "fodselsdato", required = true) val fodselsdato: LocalDate,
    val harRelasjon: Boolean = true,
    val skalFinnesIPDL: Boolean = true,
)

data class InntektPerÅrDto(val år: Int, val beløp: Beløp) {
    fun to(): InntektPerÅr {
        return InntektPerÅr(Year.of(år), beløp)
    }
}

data class TestFastlege(
    val harFastlege: Boolean = false,
    val harEndretFastlege: Boolean = false,
    val varFastlegeRiktigPåSøknadstidspunkt: Boolean = true,
    val harOppgittAndreBehandlere: Boolean = false,
)
