package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.Institusjonsopphold.Institusjon
import no.nav.aap.tidslinje.Segment
import java.time.LocalDate

data class SoningsoppholdDto(
    val institusjonstype: String,
    val institusjonsopphold: String,
    val status: String,
    val oppholdFra: LocalDate,
    val avsluttetDato: LocalDate?,
    val kildeInstitusjon: String,
    ) {
    companion object {
        fun institusjonToDto(institusjonsopphold: Segment<Institusjon>) =
            SoningsoppholdDto(
                institusjonstype = institusjonsopphold.verdi.type.beskrivelse,
                institusjonsopphold = institusjonsopphold.verdi.kategori.beskrivelse,
                status = "Ukjent",  // TODO finn ut hva som skal være her
                kildeInstitusjon = institusjonsopphold.verdi.orgnr,
                oppholdFra = institusjonsopphold.periode.fom,
                avsluttetDato = institusjonsopphold.periode.tom
            )
    }
}
