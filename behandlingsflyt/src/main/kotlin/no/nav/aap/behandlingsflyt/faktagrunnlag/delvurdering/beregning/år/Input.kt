package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år

import no.nav.aap.behandlingsflyt.behandling.beregning.InntektsPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskader
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import java.time.LocalDate

/**
 * @param nedsettelsesDato Dato da arbeidsevnen ble nedsatt.
 * @param årsInntekter Inntekter per år.
 * @param uføregrad Uføregrader bruker har hatt de siste tre årene.
 * @param yrkesskadevurdering Hvis ikke-null, en yrkesskadevurdering.
 * @param registrerteYrkesskader
 * @param beregningGrunnlag Se [no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag]
 * @param inntektsPerioder Inntekt per måned
 */
data class Input(
    val nedsettelsesDato: LocalDate,
    val årsInntekter: Set<InntektPerÅr>,
    val inntektsPerioder: List<InntektsPeriode>,
    val uføregrad: Set<Uføre>, // TODO: ta hensyn til stopp av uføre?
    val yrkesskadevurdering: Yrkesskadevurdering?,
    val registrerteYrkesskader: Yrkesskader?,
    val beregningGrunnlag: BeregningGrunnlag?,
)
