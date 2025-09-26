package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskader
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import java.time.LocalDate

/**
 * @param nedsettelsesDato Dato da arbeidsevnen ble nedsatt.
 * @param inntekter Inntekter per år.
 * @param uføregrad Hvis ikke-null, uføregrad i prosent.
 * @param yrkesskadevurdering Hvis ikke-null, en yrkesskadevurdering.
 * @param registrerteYrkesskader
 * @param beregningGrunnlag Se [no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag]
 */
data class Input(
    val nedsettelsesDato: LocalDate,
    val inntekter: Set<InntektPerÅr>,
    val uføregrad: List<Uføre>,
    val yrkesskadevurdering: Yrkesskadevurdering?,
    val registrerteYrkesskader: Yrkesskader?,
    val beregningGrunnlag: BeregningGrunnlag?,
)