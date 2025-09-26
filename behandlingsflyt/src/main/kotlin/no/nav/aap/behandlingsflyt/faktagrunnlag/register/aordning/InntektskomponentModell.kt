package no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning

import java.time.LocalDate
import java.time.YearMonth

data class InntektskomponentData(
    val arbeidsInntektMaaned: List<ArbeidsInntektMaaned> = emptyList()
)

data class ArbeidsInntektMaaned(
    val aarMaaned: YearMonth,
    val arbeidsInntektInformasjon: ArbeidsInntektInformasjon
)

data class ArbeidsInntektInformasjon(
    val inntektListe: List<Inntekt>
)

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