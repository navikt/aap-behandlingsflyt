package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode

import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import java.time.LocalDate
import java.time.LocalDateTime

data class RettighetsperiodeVurdering(
    val startDato: LocalDate?,
    val begrunnelse: String,
    val harRettUtoverSøknadsdato: Boolean,
    val harKravPåRenter: Boolean?,
    val vurdertAv: String,
    val vurdertDato: LocalDateTime? = null
)

data class RettighetsperiodeVurderingDTO(
    val startDato: LocalDate?,
    val begrunnelse: String,
    val harRettUtoverSøknadsdato: Boolean,
    val harKravPåRenter: Boolean?
) {
    init {
        if (harRettUtoverSøknadsdato == true) {
            if (startDato == null) {
                throw UgyldigForespørselException("Må sette startdato når bruker har rett utover søknadsdatoen")
            }
            if (harKravPåRenter == null) {
                throw UgyldigForespørselException("Må vurdere renter når bruker har rett utover søknadsdatoen")
            }
        }
        if (harRettUtoverSøknadsdato == false) {
            if (startDato != null) {
                throw UgyldigForespørselException("Kan ikke sette startdato når bruker ikke har rett utover søknadsdatoen")
            }
            if (harKravPåRenter != null) {
                throw UgyldigForespørselException("Kan ikke vurdere renter når bruker ikke har rett utover søknadsdatoen")
            }
        }
    }
}