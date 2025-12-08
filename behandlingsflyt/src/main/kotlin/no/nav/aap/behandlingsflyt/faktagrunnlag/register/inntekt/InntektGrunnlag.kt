package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import no.nav.aap.behandlingsflyt.behandling.beregning.InntektsPeriode

data class InntektGrunnlag(
    val inntekter: Set<InntektPerÅr>,
    val inntektPerMåned: Set<InntektsPeriode>
)