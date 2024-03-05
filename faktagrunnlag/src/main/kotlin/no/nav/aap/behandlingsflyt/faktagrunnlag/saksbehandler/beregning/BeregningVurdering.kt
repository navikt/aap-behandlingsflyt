package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.verdityper.Beløp
import java.math.BigDecimal
import java.time.LocalDate

data class BeregningVurderingDto(
    val begrunnelse: String,
    val nedsattArbeidsevneDato: LocalDate,
    val ytterligereNedsattArbeidsevneDato: LocalDate?,
    val antattÅrligInntekt: BigDecimal?,
){
    fun tilBeregningVurdering(): BeregningVurdering {
        return BeregningVurdering(
            begrunnelse = begrunnelse,
            nedsattArbeidsevneDato = nedsattArbeidsevneDato,
            ytterligereNedsattArbeidsevneDato = ytterligereNedsattArbeidsevneDato,
            antattÅrligInntekt = antattÅrligInntekt?.let(::Beløp)
        )
    }
}

data class BeregningVurdering(
    val begrunnelse: String,
    val nedsattArbeidsevneDato: LocalDate,
    val ytterligereNedsattArbeidsevneDato: LocalDate?,
    val antattÅrligInntekt: Beløp?
) {
    fun utledInput(): Inntektsbehov {
        return Inntektsbehov(
            Input(
                nedsettelsesDato = nedsattArbeidsevneDato,
                ytterligereNedsettelsesDato = ytterligereNedsattArbeidsevneDato
            )
        )
    }
}