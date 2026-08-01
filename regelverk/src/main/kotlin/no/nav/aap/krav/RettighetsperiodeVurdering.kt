package no.nav.aap.krav

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.aap.komponenter.verdityper.Bruker

data class RettighetsperiodeVurdering(
    val startDato: LocalDate?,
    val begrunnelse: String,
    val harRettUtoverSøknadsdato: RettighetsperiodeHarRett,
    val vurdertAv: Bruker,
    val vurdertDato: LocalDateTime
)

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