package no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt

class InntektGrunnlag(
    private val id: Long?,
    val inntekter: Set<InntektPerÅr>
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InntektGrunnlag

        return inntekter == other.inntekter
    }

    override fun hashCode(): Int {
        return inntekter.hashCode()
    }
}