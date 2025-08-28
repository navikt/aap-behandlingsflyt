package no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning

import java.time.LocalDate
import java.time.YearMonth

// TODO: her burde respons fra eksternt API og hva som lagres i db være forskjellige klasser

data class InntektskomponentResponse(
    val arbeidsInntektMaaned: List<ArbeidsInntektMaaned> = emptyList()
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