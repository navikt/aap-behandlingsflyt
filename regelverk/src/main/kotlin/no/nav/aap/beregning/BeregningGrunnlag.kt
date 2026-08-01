package no.nav.aap.beregning

data class BeregningGrunnlag(
    val tidspunktVurdering: BeregningstidspunktVurdering?,
    val yrkesskadeBeløpVurdering: BeregningYrkeskaderBeløpVurdering?
)