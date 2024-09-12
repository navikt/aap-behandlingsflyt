package no.nav.aap.Inntekt

class InntektRequest(
    val fnr: String,
    val fomAr: Int,
    val tomAr: Int
)

class InntektResponse(
    val inntekter: List<SumPi>
)

class SumPi(
    val inntektAr: Int,
    val belop: Long, //TODO: Vi prøver å se om belop klarer seg uten nullable
    val inntektType: String
)
