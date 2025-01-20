package no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning

import java.time.YearMonth

data class InntektskomponentResponse(
    val arbeidsInntektMaaned: List<Måned> = listOf()
)

data class Måned(
    val aarMaaned: YearMonth,
    val arbeidsInntektInformasjon: ArbeidsInntektInformasjon
)

data class ArbeidsInntektInformasjon(
    val inntektListe: List<Inntekt>
)

data class Inntekt(
    val beloep: Double,
    val opptjeningsland: String?,
    val skattemessigBosattLand: String?
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