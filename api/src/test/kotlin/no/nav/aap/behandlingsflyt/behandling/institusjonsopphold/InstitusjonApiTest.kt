package no.nav.aap.behandlingsflyt.behandling.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdene
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.*

class InstitusjonApiTest {

    @Nested
    @DisplayName("Tester funksjonen byggTidslinjeForInstitusjonsopphold")
    inner class ByggTidslinjeForOpphold {
        @Test
        fun `byggTidslinjeForInstitusjonsopphold returnerer tom tidslinje når grunnlag er null`() {
            val tidslinje = byggTidslinjeForInstitusjonsopphold(null, Institusjonstype.HS)

            assertThat(tidslinje.segmenter()).isEmpty()
        }

        @Test
        fun `byggTidslinjeForInstitusjonsopphold returnerer tom tidslinje når ingen opphold finnes`() {
            val grunnlag = InstitusjonsoppholdGrunnlag(
                oppholdene = Oppholdene(id = 1L, opphold = emptyList())
            )

            val tidslinje = byggTidslinjeForInstitusjonsopphold(grunnlag, Institusjonstype.HS)

            assertThat(tidslinje.segmenter()).isEmpty()
        }

        @Test
        fun `byggTidslinjeForInstitusjonsopphold filtrerer kun på angitt institusjonstype`() {
            val oppholdHelseinst = lagSegment(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 31),
                type = Institusjonstype.HS
            )
            val oppholdFengsel = lagSegment(
                fom = LocalDate.of(2024, 2, 1),
                tom = LocalDate.of(2024, 2, 28),
                type = Institusjonstype.FO
            )
            val grunnlag = InstitusjonsoppholdGrunnlag(
                oppholdene = Oppholdene(id = 1L, opphold = listOf(oppholdHelseinst, oppholdFengsel))
            )

            val tidslinje = byggTidslinjeForInstitusjonsopphold(grunnlag, Institusjonstype.HS)

            assertThat(tidslinje.segmenter()).hasSize(1)

            val segment = tidslinje.segmenter().single()
            assertThat(segment.verdi.type).isEqualTo(Institusjonstype.HS)
            assertThat(segment.fom()).isEqualTo(oppholdHelseinst.fom())
            assertThat(segment.tom()).isEqualTo(oppholdHelseinst.tom())
        }

        @Test
        fun `byggTidslinjeForInstitusjonsopphold justerer ikke perioder som ikke overlapper`() {
            val opphold1 = lagSegment(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 15),
                type = Institusjonstype.HS,
            )
            val opphold2 = lagSegment(
                fom = LocalDate.of(2024, 1, 20),
                tom = LocalDate.of(2024, 1, 31),
                type = Institusjonstype.HS,
            )
            val grunnlag = InstitusjonsoppholdGrunnlag(
                oppholdene = Oppholdene(id = 1L, opphold = listOf(opphold1, opphold2))
            )

            val tidslinje = byggTidslinjeForInstitusjonsopphold(grunnlag, Institusjonstype.HS)
            val segmenter = tidslinje.segmenter().toList()

            assertThat(segmenter).hasSize(2)
            assertThat(segmenter[0].periode.fom).isEqualTo(opphold1.fom())
            assertThat(segmenter[0].periode.tom).isEqualTo(opphold1.tom())
            assertThat(segmenter[1].periode.fom).isEqualTo(opphold2.fom())
            assertThat(segmenter[1].periode.tom).isEqualTo(opphold2.tom())
        }

        @Test
        fun `byggTidslinjeForInstitusjonsopphold justerer perioder som starter samme dag som forrige slutter`() {
            val opphold1 = lagSegment(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 15),
                type = Institusjonstype.HS,
            )
            val opphold2 = lagSegment(
                fom = LocalDate.of(2024, 1, 15),
                tom = LocalDate.of(2024, 1, 31),
                type = Institusjonstype.HS,
            )
            val grunnlag = InstitusjonsoppholdGrunnlag(
                oppholdene = Oppholdene(id = 1L, opphold = listOf(opphold1, opphold2))
            )

            val tidslinje = byggTidslinjeForInstitusjonsopphold(grunnlag, Institusjonstype.HS)
            val segmenter = tidslinje.segmenter().toList()

            assertThat(segmenter).hasSize(2)
            assertThat(segmenter[0].periode.fom).isEqualTo(opphold1.fom())
            // Første opphold skal få tom justert til dagen før neste starter
            assertThat(segmenter[0].periode.tom).isEqualTo(opphold1.tom().minusDays(1))
            assertThat(segmenter[1].periode.fom).isEqualTo(opphold2.fom())
            assertThat(segmenter[1].periode.tom).isEqualTo(opphold2.tom())
        }

        @Test
        fun `byggTidslinjeForInstitusjonsopphold kaster feil på perioder som har mer enn en dags overlapp`() {
            val opphold1 = lagSegment(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 12),
                type = Institusjonstype.HS,
            )
            val opphold2 = lagSegment(
                fom = LocalDate.of(2024, 1, 11),
                tom = LocalDate.of(2024, 1, 31),
                type = Institusjonstype.HS,
            )
            val grunnlag = InstitusjonsoppholdGrunnlag(
                oppholdene = Oppholdene(id = 1L, opphold = listOf(opphold1, opphold2))
            )

            assertThrows<IllegalArgumentException> {
                byggTidslinjeForInstitusjonsopphold(grunnlag, Institusjonstype.HS)
            }
        }

        @Test
        fun `byggTidslinjeForInstitusjonsopphold håndterer flere sammenhengende perioder`() {
            val opphold1 = lagSegment(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 10),
                type = Institusjonstype.HS
            )
            val opphold2 = lagSegment(
                fom = LocalDate.of(2024, 1, 10),
                tom = LocalDate.of(2024, 1, 20),
                type = Institusjonstype.HS
            )
            val opphold3 = lagSegment(
                fom = LocalDate.of(2024, 1, 20),
                tom = LocalDate.of(2024, 1, 31),
                type = Institusjonstype.HS
            )
            val grunnlag = InstitusjonsoppholdGrunnlag(
                oppholdene = Oppholdene(id = 1L, opphold = listOf(opphold1, opphold2, opphold3))
            )

            val tidslinje = byggTidslinjeForInstitusjonsopphold(grunnlag, Institusjonstype.HS)
            val segmenter = tidslinje.segmenter().toList()

            assertThat(segmenter).hasSize(3)
            assertThat(segmenter[0].periode.tom).isEqualTo(opphold1.tom().minusDays(1))
            assertThat(segmenter[1].periode.tom).isEqualTo(opphold2.tom().minusDays(1))
            assertThat(segmenter[2].periode.tom).isEqualTo(opphold3.tom())
        }

    }

    private fun lagSegment(
        fom: LocalDate,
        tom: LocalDate,
        type: Institusjonstype,
    ): Segment<Institusjon> {
        return Segment(
            periode = Periode(fom, tom),
            verdi = Institusjon(
                type = type,
                kategori = if (type == Institusjonstype.HS) Oppholdstype.H else Oppholdstype.S,
                orgnr = "123456789",
                navn = UUID.randomUUID().toString()
            )
        )
    }
}
