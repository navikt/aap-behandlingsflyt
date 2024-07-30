package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.tidslinje.Segment
import java.time.LocalDate

data class InstitusjonsoppholdDto(
    val institusjonstype: String,
    val oppholdstype: String,
    val status: String,
    val oppholdFra: LocalDate,
    val avsluttetDato: LocalDate?,
    val kildeinstitusjon: String,
    ) {
    companion object {
        fun institusjonToDto(institusjonsopphold: Segment<Institusjon>) =
            InstitusjonsoppholdDto(
                institusjonstype = institusjonsopphold.verdi.type.beskrivelse,
                oppholdstype = institusjonsopphold.verdi.kategori.beskrivelse,
                status = "Ukjent",  // TODO finn ut hva som skal være her
                kildeinstitusjon = institusjonsopphold.verdi.navn,
                oppholdFra = institusjonsopphold.periode.fom,
                avsluttetDato = institusjonsopphold.periode.tom
            )
    }
}
