package no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning

import java.time.YearMonth

data class InntektskomponentResponse(
    val arbeidsInntektMaaned: List<Måned>
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
)

data class InntektskomponentRequest(
    val maanedFom: YearMonth,
    val maanedTom: YearMonth,
    val ident: Ident,
    val formaal: String = "ArbeidsavklaringspengerA-inntekt",
    val ainntektsfilter: String = "arbeidsavklaringspenger",
)

data class Ident(
    val identifikator: String,
    val aktoerType: String = "NATURLIG_IDENT"
)