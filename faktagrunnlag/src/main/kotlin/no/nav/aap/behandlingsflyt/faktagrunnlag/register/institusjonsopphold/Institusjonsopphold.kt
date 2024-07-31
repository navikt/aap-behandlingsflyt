package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.verdityper.Periode
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
        return Periode(startdato, sluttdato ?: LocalDate.MAX)
    }

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