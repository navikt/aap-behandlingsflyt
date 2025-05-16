package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurdering

data class ManuellInntektGrunnlag(
    val manuelleInntekter: Set<ManuellInntektVurdering>
)