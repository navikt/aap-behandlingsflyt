package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode

import java.time.LocalDate

data class RettighetsperiodeVurdering(
    val startDato: LocalDate,
    val begrunnelse: String,
    val harRettUtoverSøknadsdato: Boolean,
    val harKravPåRenter: Boolean,
)