package no.nav.aap.behandlingsflyt.avklaringsbehov.meldeplikt

import java.time.Period

data class Fritaksvurdering(val period: Period, val begrunnelse: String, val harFritak: Boolean)
