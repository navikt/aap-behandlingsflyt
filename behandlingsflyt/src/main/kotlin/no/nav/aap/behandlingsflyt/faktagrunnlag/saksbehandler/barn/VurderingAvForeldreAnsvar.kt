package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn

import java.time.LocalDate

data class VurderingAvForeldreAnsvar(val fraDato: LocalDate, val harForeldreAnsvar: Boolean, val begrunnelse: String, val erFosterForelder: Boolean? = null)