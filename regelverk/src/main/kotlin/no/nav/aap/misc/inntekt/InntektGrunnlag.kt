package no.nav.aap.misc.inntekt

import no.nav.aap.beregning.InntektPerÅr
import no.nav.aap.beregning.Månedsinntekt

data class InntektGrunnlag(
    val inntekter: Set<InntektPerÅr>,
    val inntektPerMåned: Set<Månedsinntekt>
)