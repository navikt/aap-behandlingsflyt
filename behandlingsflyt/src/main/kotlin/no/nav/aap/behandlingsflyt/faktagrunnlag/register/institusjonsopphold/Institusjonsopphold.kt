package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

class Institusjonsopphold(
    val institusjonstype: Institusjonstype,
    val kategori: Oppholdstype,
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
    val orgnr: String? = null,
    val institusjonsnavn: String
) {
    fun periode(): Periode {
        return Periode(startdato, sluttdato ?: Tid.MAKS)
    }

    fun tilInstitusjonSegment(): Segment<Institusjon> =
        Segment(
            periode(),
            Institusjon(
                type = institusjonstype,
                kategori = kategori,
                orgnr = requireNotNull(orgnr) {
                    """Orgnr er nullable i følge inst2-swagger, men databasen krever not null.
                        | Dette burde kanskjehåndteres en dag?""".trimMargin()
                },
                navn = institusjonsnavn,
            )
        )

    companion object {
        fun nyttOpphold(
            institusjonstype: String,
            kategori: String,
            startdato: LocalDate,
            sluttdato: LocalDate?,
            orgnr: String?,
            institusjonsnavn: String
        ): Institusjonsopphold {
            return Institusjonsopphold(
                Institusjonstype.valueOf(institusjonstype),
                Oppholdstype.valueOf(kategori),
                startdato,
                sluttdato,
                orgnr,
                institusjonsnavn
            )
        }
    }
}