package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.verdityper.Prosent
import java.time.LocalDate

/**
 * @param nedsettelsesDato Dato da arbeidsevnen ble nedsatt.
 * @param inntekter Inntekter per år.
 * @param uføregrad Hvis ikke-null, uføregrad i prosent.
 * @param yrkesskadevurdering Hvis ikke-null, en yrkesskadevurdering.
 * @param beregningVurdering Se [BeregningVurdering].
 */
data class Input(
    val nedsettelsesDato: LocalDate,
    val inntekter: Set<InntektPerÅr>,
    val uføregrad: Prosent?,
    val yrkesskadevurdering: Yrkesskadevurdering?,
    val beregningVurdering: BeregningVurdering?
) {

    fun datoerForInnhenting(): Set<LocalDate> {
        val ytterligereNedsattArbeidsevneDato = beregningVurdering?.ytterligereNedsattArbeidsevneDato
        if (ytterligereNedsattArbeidsevneDato == null) {
            return setOf(nedsettelsesDato)
        }

        return setOf(nedsettelsesDato, ytterligereNedsattArbeidsevneDato)
    }
}
