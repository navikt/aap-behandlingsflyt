package no.nav.aap.behandlingsflyt.behandling.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonsopphold
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InstitusjonsoppholdFinnRelevanteOppholdTest {

    private fun opphold(
        fom: LocalDate,
        tom: LocalDate?,
        institusjonsnavn: String = "Testinstitusjon"
    ) = Institusjonsopphold(
        institusjonstype = Institusjonstype.HS,
        kategori = Oppholdstype.H,
        startdato = fom,
        sluttdato = tom,
        orgnr = "111222333",
        institusjonsnavn = institusjonsnavn
    )

    @Test
    fun `opphold som slutter samme dag som neste starter skal begge inkluderes selv om kun det siste overlapper rettighetsperioden`() {
        // Opphold A: 01.01.26-01.02.26 hos institusjon A. Opphold B: 01.02.26-pågående hos institusjon B.
        // Rettighetsperioden starter først 01.03.26, altså etter at opphold A er avsluttet alene,
        // men opphold A og B er sammenhengende siden B starter samme dag som A slutter.
        val oppholdA = opphold(1 januar 2026, 1 februar 2026, institusjonsnavn = "Institusjon A")
        val oppholdB = opphold(1 februar 2026, null, institusjonsnavn = "Institusjon B")
        val rettighetsperiode = Periode(1 mars 2026, Tid.MAKS)

        val resultat = InstitusjonsoppholdInformasjonskrav.finnRelevanteOpphold(
            listOf(oppholdA, oppholdB),
            rettighetsperiode
        )

        assertThat(resultat).containsExactlyInAnyOrder(oppholdA, oppholdB)
    }

    @Test
    fun `opphold med reelt gap på ett døgn skal ikke regnes som sammenhengende`() {
        // Opphold A slutter 01.02.26. Opphold B starter 03.02.26 - et helt døgns gap (02.02.26).
        val oppholdA = opphold(1 januar 2026, 1 februar 2026)
        val oppholdB = opphold(3 februar 2026, null)
        val rettighetsperiode = Periode(1 mars 2026, Tid.MAKS)

        val resultat = InstitusjonsoppholdInformasjonskrav.finnRelevanteOpphold(
            listOf(oppholdA, oppholdB),
            rettighetsperiode
        )

        assertThat(resultat).containsExactly(oppholdB)
    }

    @Test
    fun `kjede med tre sammenhengende opphold inkluderes selv om kun det siste overlapper rettighetsperioden`() {
        val oppholdA = opphold(1 januar 2026, 15 januar 2026, institusjonsnavn = "A")
        val oppholdB = opphold(15 januar 2026, 15 februar 2026, institusjonsnavn = "B")
        val oppholdC = opphold(1 februar 2026, null, institusjonsnavn = "C")
        val rettighetsperiode = Periode(1 mars 2026, Tid.MAKS)

        val resultat = InstitusjonsoppholdInformasjonskrav.finnRelevanteOpphold(
            listOf(oppholdB, oppholdA, oppholdC), // usortert input
            rettighetsperiode
        )

        assertThat(resultat).containsExactlyInAnyOrder(oppholdA, oppholdB, oppholdC)
    }

    @Test
    fun `tom liste gir tomt resultat`() {
        val resultat = InstitusjonsoppholdInformasjonskrav.finnRelevanteOpphold(
            emptyList(),
            Periode(1 mars 2026, Tid.MAKS)
        )

        assertThat(resultat).isEmpty()
    }
}
