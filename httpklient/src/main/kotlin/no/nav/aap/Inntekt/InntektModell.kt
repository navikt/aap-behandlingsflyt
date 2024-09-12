package no.nav.aap.Inntekt

class InntektRequest (
    var fnr: String,
    var fomAr: Int,
    var tomAr: Int
)

class InntektResponse (
    val inntekter: List<SumPi> = emptyList()
    )

class SumPi(
    val inntektAr: Int,
    val belop: Long, //TODO: Vi prøver å se om belop klarer seg uten nullable
    val inntektType: String
    )