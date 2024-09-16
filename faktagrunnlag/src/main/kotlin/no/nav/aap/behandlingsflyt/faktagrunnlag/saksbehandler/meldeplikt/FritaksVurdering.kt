package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.komponenter.type.Periode

data class Fritaksvurdering(
    val fritaksPerioder: List<FritaksPeriode>,
    val begrunnelse: String,
)

data class FritaksPeriode(
    val periode: Periode,
    val harFritak: Boolean
)