package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.komponenter.type.Periode

data class Fritaksvurdering(
    val fritaksPerioder: List<FritaksPeriode>,
    val begrunnelse: String,
) {
    operator fun plus(other: Fritaksvurdering): Fritaksvurdering {
        if (begrunnelse != other.begrunnelse) throw IllegalStateException("Begrunnelse must be the same to merge")
        return Fritaksvurdering(fritaksPerioder + other.fritaksPerioder, begrunnelse,)
    }
}

data class FritaksPeriode(
    val periode: Periode,
    val harFritak: Boolean
)