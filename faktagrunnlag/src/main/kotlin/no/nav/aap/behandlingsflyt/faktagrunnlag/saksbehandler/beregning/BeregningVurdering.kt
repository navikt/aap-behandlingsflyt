package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.verdityper.Beløp
import java.math.BigDecimal
import java.time.LocalDate

data class BeregningVurderingDto(
    val begrunnelse: String,
    val ytterligereNedsattArbeidsevneDato: LocalDate?,
    val antattÅrligInntekt: BigDecimal?,
) {
    fun tilBeregningVurdering(): BeregningVurdering {
        return BeregningVurdering(
            begrunnelse = begrunnelse,
            ytterligereNedsattArbeidsevneDato = ytterligereNedsattArbeidsevneDato,
            antattÅrligInntekt = antattÅrligInntekt?.let(::Beløp)
        )
    }
}

data class BeregningVurdering(
    val begrunnelse: String,
    val ytterligereNedsattArbeidsevneDato: LocalDate?,
    val antattÅrligInntekt: Beløp?
) {
    fun utledInput(sykdomsvurdering: Sykdomsvurdering): Inntektsbehov {
        return Inntektsbehov(
            Input(
                nedsettelsesDato = requireNotNull(sykdomsvurdering.nedsattArbeidsevneDato).atMonth(1).atDay(1),
                ytterligereNedsettelsesDato = ytterligereNedsattArbeidsevneDato
            )
        )
    }
}