package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.behandlingsflyt.test.august
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.november
import no.nav.aap.behandlingsflyt.test.september
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class InstitusjonsoppholdServiceTest {

    @Test
    fun `Ulik rekkefølge i lister skal ikke gi endring`() {
        val eksisterendeGrunnlag = InstitusjonsoppholdGrunnlag(
            Oppholdene(
                1, listOf(
                    Institusjonsopphold(
                        Institusjonstype.AS,
                        Oppholdstype.A,
                        1 juni 2020,
                        1 august 2020,
                        orgnr = "123",
                        institusjonsnavn = "Navn 1"
                    ),
                    Institusjonsopphold(
                        Institusjonstype.AS,
                        Oppholdstype.D,
                        1 september 2020,
                        1 november 2020,
                        orgnr = "123",
                        institusjonsnavn = "Navn 2"
                    ),
                ).map { it.tilInstitusjonSegment() })
        )

        val ny = listOf(
            Institusjonsopphold(
                Institusjonstype.AS,
                Oppholdstype.D,
                1 september 2020,
                1 november 2020,
                orgnr = "123",
                institusjonsnavn = "Navn 2"
            ), Institusjonsopphold(
                Institusjonstype.AS,
                Oppholdstype.A,
                1 juni 2020,
                1 august 2020,
                orgnr = "123",
                institusjonsnavn = "Navn 1"
            )
        )

        assertFalse(InstitusjonsoppholdService.erEndret(eksisterendeGrunnlag, ny))
    }

}