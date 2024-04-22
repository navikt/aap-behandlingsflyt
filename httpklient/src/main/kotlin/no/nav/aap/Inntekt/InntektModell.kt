package no.nav.aap.Inntekt

class InntektRequest (
    var fnr: String,
    var fomAr: Int,
    var tomAr: Int
)

class InntektResponse {
    val inntekt: List<sumPi> = emptyList()
}

class sumPi(
    val inntektAr: Int?,
    val belop: Long?,
    val inntektType: String?
    )