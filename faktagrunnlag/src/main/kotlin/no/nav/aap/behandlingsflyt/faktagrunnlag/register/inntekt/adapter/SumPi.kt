package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.adapter

class SumPi(
    val inntektAr: Int,
    val belop: Long, //TODO: Vi prøver å se om belop klarer seg uten nullable
    val inntektType: String
)