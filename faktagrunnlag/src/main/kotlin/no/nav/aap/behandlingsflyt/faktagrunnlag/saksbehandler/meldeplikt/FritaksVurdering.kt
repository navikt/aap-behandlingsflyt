package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.komponenter.type.Periode

data class Fritaksvurdering(
    val fritaksperioder: List<Fritaksperiode>,
    val begrunnelse: String,
)

data class Fritaksperiode(
    val periode: Periode,
    val harFritak: Boolean
)