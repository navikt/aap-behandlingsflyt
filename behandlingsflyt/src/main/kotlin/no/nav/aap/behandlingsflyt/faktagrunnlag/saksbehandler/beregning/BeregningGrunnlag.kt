package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

data class BeregningGrunnlag(
    val tidspunktVurdering: BeregningstidspunktVurdering?,
    val yrkesskadeBeløpVurdering: BeregningYrkeskaderBeløpVurdering?
)