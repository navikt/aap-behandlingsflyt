package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode

import java.time.LocalDate

data class RettighetsperiodeVurdering(
    val startDato: LocalDate?,
    val begrunnelse: String,
    val harRettUtoverSøknadsdato: Boolean,
    val harKravPåRenter: Boolean?,
) {
    init {
        if (harRettUtoverSøknadsdato == true) {
            require(startDato != null) { "Må sette startdato når bruker har rett utover søknadsdatoen" }
            require(harKravPåRenter != null) { "Må vurdere renter når bruker har rett utover søknadsdatoen" }
        }
        if (harRettUtoverSøknadsdato == false) {
            require(startDato == null) { "Kan ikke sette startdato når bruker ikke har rett utover søknadsdatoen" }
            require(harKravPåRenter == null) { "Kan ikke vurdere renter når bruker ikke har rett utover søknadsdatoen" }
        }
    }
}