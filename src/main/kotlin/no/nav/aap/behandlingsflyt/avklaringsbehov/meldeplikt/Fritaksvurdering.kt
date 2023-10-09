package no.nav.aap.behandlingsflyt.avklaringsbehov.meldeplikt

import no.nav.aap.behandlingsflyt.domene.Periode

data class Fritaksvurdering(val periode: Periode, val begrunnelse: String, val harFritak: Boolean)
