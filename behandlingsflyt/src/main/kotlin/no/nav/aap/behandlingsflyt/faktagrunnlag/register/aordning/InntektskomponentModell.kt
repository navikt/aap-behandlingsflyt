package no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning

import java.time.LocalDate
import java.time.YearMonth

data class InntektskomponentResponse(
    val arbeidsInntektMaaned: List<ArbeidsInntektMaaned> = listOf()
)

data class ArbeidsInntektMaaned(
    val aarMaaned: YearMonth,
    val arbeidsInntektInformasjon: ArbeidsInntektInformasjon
)

data class ArbeidsInntektInformasjon(
    val inntektListe: List<Inntekt>
) {
    init {
        require(inntektListe.isNotEmpty()) { "Inntektliste kan ikke være tom" }
    }
}

data class Inntekt(
    val beloep: Double,
    val opptjeningsland: String?,
    val skattemessigBosattLand: String?,
    val opptjeningsperiodeFom: LocalDate?,
    val opptjeningsperiodeTom: LocalDate?,
    val virksomhet: Virksomhet,
    val beskrivelse: String?
)

data class Virksomhet(
    val identifikator: String,
)

data class InntektskomponentRequest(
    val maanedFom: YearMonth,
    val maanedTom: YearMonth,
    val ident: Ident,
    val formaal: String = "Arbeidsavklaringspenger",
    val ainntektsfilter: String = "ArbeidsavklaringspengerA-inntekt"
)

data class Ident(
    val identifikator: String,
    val aktoerType: String = "NATURLIG_IDENT"
)