package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.komponenter.type.Periode

data class Fritaksvurdering(
    val periode: Periode,
    val begrunnelse: String,
    val harFritak: Boolean
)
