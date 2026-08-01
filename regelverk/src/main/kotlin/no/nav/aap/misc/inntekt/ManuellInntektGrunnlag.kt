package no.nav.aap.misc.inntekt

import no.nav.aap.beregning.ManuellInntektVurdering

data class ManuellInntektGrunnlag(
    val manuelleInntekter: Set<ManuellInntektVurdering>
)