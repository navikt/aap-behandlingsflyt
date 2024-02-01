package no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.beregning.år.Input

class SykdomGrunnlag(
    private val id: Long?,
    val yrkesskadevurdering: Yrkesskadevurdering?,
    val sykdomsvurdering: Sykdomsvurdering?
) {
    fun utledInput(): Inntektsbehov {
        return Inntektsbehov(
            Input(
                nedsettelsesDato = sykdomsvurdering?.nedsattArbeidsevneDato!!,
                ytterligereNedsettelsesDato = sykdomsvurdering.ytterligereNedsattArbeidsevneDato
            )
        )
    }

    fun erKonsistent(): Boolean {
        if (sykdomsvurdering == null) {
            return false
        }
        if (yrkesskadevurdering?.erÅrsakssammenheng == true) {
            return sykdomsvurdering.nedreGrense == NedreGrense.TRETTI
        }
        return sykdomsvurdering.nedreGrense == NedreGrense.FEMTI
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SykdomGrunnlag

        if (yrkesskadevurdering != other.yrkesskadevurdering) return false
        if (sykdomsvurdering != other.sykdomsvurdering) return false

        return true
    }

    override fun hashCode(): Int {
        var result = yrkesskadevurdering?.hashCode() ?: 0
        result = 31 * result + (sykdomsvurdering?.hashCode() ?: 0)
        return result
    }

    fun id(): Long {
        return requireNotNull(id)
    }

}
