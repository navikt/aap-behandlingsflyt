package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.OverstyrMuligRettFraÅrsak
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import java.time.LocalDate
import java.time.LocalDateTime

data class RettighetsperiodeVurdering(
    val startDato: LocalDate?,
    val begrunnelse: String,
    val harRettUtoverSøknadsdato: RettighetsperiodeHarRett,
    val vurdertAv: String,
    val vurdertDato: LocalDateTime
)

data class RettighetsperiodeVurderingDTO(
    val startDato: LocalDate?,
    val begrunnelse: String,
    val harRett: RettighetsperiodeHarRett
) {
    init {
        if (harRett.harOverstyrt() && startDato == null) {
            throw UgyldigForespørselException("Må sette startdato når bruker har rett utover søknadsdatoen")
        }
        if (!harRett.harOverstyrt() && startDato != null) {
            throw UgyldigForespørselException("Kan ikke sette startdato når bruker ikke har rett utover søknadsdatoen")
        }
    }
}

enum class RettighetsperiodeHarRett {
    Ja, // For bakoverkompabikitet fra når feltet var boolean. Vi kan ikke vite hvilken av de 2 "har rett" true skal mappes til
    Nei,
    HarRettIkkeIStandTilÅSøkeTidligere,
    HarRettMisvisendeOpplysninger;

    fun harOverstyrt(): Boolean {
        return when (this) {
            Ja, HarRettIkkeIStandTilÅSøkeTidligere, HarRettMisvisendeOpplysninger -> true
            Nei -> false
        }
    }

    fun tilOverstyrMuligRettFraÅrsak(): OverstyrMuligRettFraÅrsak {
        return when (this) {
            Ja, Nei -> throw IllegalArgumentException("Mulig rett fra kan ikke utledes fra disse verdiene")
            HarRettIkkeIStandTilÅSøkeTidligere -> OverstyrMuligRettFraÅrsak.IkkeIStandTilÅSøkeTidligere
            HarRettMisvisendeOpplysninger -> OverstyrMuligRettFraÅrsak.MisvisendeOpplysninger
        }
    }

    fun kanUtledeOverstyrMuligRettFraÅrsak(): Boolean {
        return when (this) {
            HarRettIkkeIStandTilÅSøkeTidligere,
            HarRettMisvisendeOpplysninger -> true

            else -> false
        }
    }
}