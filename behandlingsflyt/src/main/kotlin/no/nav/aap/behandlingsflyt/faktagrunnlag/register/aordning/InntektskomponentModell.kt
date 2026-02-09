package no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning

import java.time.LocalDate
import java.time.YearMonth

data class InntektskomponentData(
    val arbeidsInntektMåned: List<ArbeidsInntektMåned> = emptyList()
)

data class ArbeidsInntektMåned(
    val årMåned: YearMonth,
    val arbeidsInntektInformasjon: ArbeidsInntektInformasjon
)

data class ArbeidsInntektInformasjon(
    val inntektListe: List<Inntekt>
)

data class Inntekt(
    val beløp: Double,
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