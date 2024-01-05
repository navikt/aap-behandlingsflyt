package no.nav.aap.behandlingsflyt.avklaringsbehov.løser.meldeplikt

import no.nav.aap.behandlingsflyt.verdityper.Periode

data class Fritaksvurdering(
    val periode: Periode,
    val begrunnelse: String,
    val harFritak: Boolean
)
