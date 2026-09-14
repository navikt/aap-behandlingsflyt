package no.nav.aap.behandlingsflyt.behandling.institusjonsopphold

import io.mockk.mockk
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdene
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.OppholdVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.august
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juli
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.behandlingsflyt.test.november
import no.nav.aap.behandlingsflyt.test.oktober
import no.nav.aap.behandlingsflyt.test.september
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*


class InstitusjonApiTest {

    @Nested
    @DisplayName("Tester funksjonen byggTidslinjeForInstitusjonsopphold")
    inner class ByggTidslinjeForOpphold {
        @Test
        fun `byggTidslinjeForInstitusjonsopphold returnerer tom tidslinje når grunnlag er null`() {
            val tidslinje = byggTidslinjeForInstitusjonsopphold(null, Institusjonstype.HS).get()

            assertThat(tidslinje.segmenter()).isEmpty()
        }

        @Test
        fun `byggTidslinjeForInstitusjonsopphold returnerer tom tidslinje når ingen opphold finnes`() {
            val grunnlag = InstitusjonsoppholdGrunnlag(
                oppholdene = Oppholdene(id = 1L, opphold = emptyList())
            )

            val tidslinje = byggTidslinjeForInstitusjonsopphold(grunnlag, Institusjonstype.HS).get()

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

            val tidslinje = byggTidslinjeForInstitusjonsopphold(grunnlag, Institusjonstype.HS).get()

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

            val tidslinje = byggTidslinjeForInstitusjonsopphold(grunnlag, Institusjonstype.HS).get()
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

            val tidslinje = byggTidslinjeForInstitusjonsopphold(grunnlag, Institusjonstype.HS).get()
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

            val result = byggTidslinjeForInstitusjonsopphold(grunnlag, Institusjonstype.HS)
            assertThat(result.isInvalid).isTrue()
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

            val tidslinje = byggTidslinjeForInstitusjonsopphold(grunnlag, Institusjonstype.HS).get()
            val segmenter = tidslinje.segmenter().toList()

            assertThat(segmenter).hasSize(3)
            assertThat(segmenter[0].periode.tom).isEqualTo(opphold1.tom().minusDays(1))
            assertThat(segmenter[1].periode.tom).isEqualTo(opphold2.tom().minusDays(1))
            assertThat(segmenter[2].periode.tom).isEqualTo(opphold3.tom())
        }
    }

    // -------------------------------------------------------------------------
    // hentOppholdSomSkalVurderes
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Tester funksjonen hentOppholdSomSkalVurderes")
    inner class HentOppholdSomSkalVurderes {
        private val sykehusA = Institusjon(Institusjonstype.HS, Oppholdstype.H, "111000111", "Sykehus A")
        private val sykehusB = Institusjon(Institusjonstype.HS, Oppholdstype.H, "222000222", "Sykehus B")
        private val sykehusC = Institusjon(Institusjonstype.HS, Oppholdstype.H, "333000333", "Sykehus C")

        private fun oppholdTidslinje(vararg segmenter: Pair<Periode, Institusjon>): Tidslinje<Institusjon> =
            segmenter.asList().somTidslinje({ it.first }, { it.second })

        private fun behovTidslinje(vararg perioder: Periode): Tidslinje<InstitusjonsoppholdVurdering> =
            Tidslinje(perioder.map { periode ->
                Segment(periode, InstitusjonsoppholdVurdering(helse = HelseOpphold(OppholdVurdering.UAVKLART)))
            })

        private fun vedtattVurdering(institusjon: Institusjon, oppholdFom: LocalDate, vurderingPeriode: Periode) =
            HelseoppholdDto(
                periode = vurderingPeriode,
                oppholdId = lagOppholdId(institusjon.navn, oppholdFom),
                vurderinger = emptyList(),
                status = OppholdVurderingDto.GODKJENT
            )

        @Test
        fun `sammenhengende opphold slås sammen til ett opphold selv om kun det siste isolert overlapper behovsperioden`() {
            val oppholdA = Segment(
                Periode(
                    4 november 2025,
                    7 januar 2026
                ), // tom justert -1 dag, som byggTidslinjeForInstitusjonsopphold gjør
                Institusjon(Institusjonstype.HS, Oppholdstype.H, "12345", "St. Mungos Hospital")
            )
            val oppholdB = Segment(
                Periode(8 januar 2026, 1 juli 2026),
                Institusjon(Institusjonstype.HS, Oppholdstype.D, "67890", "Helgelandssykehus Dialyse")
            )
            val oppholdInfo = Tidslinje(listOf(oppholdA, oppholdB))

            val behovPerioder = Tidslinje(
                listOf(
                    Segment(
                        Periode(22 januar 2026, Tid.MAKS),
                        InstitusjonsoppholdVurdering(helse = HelseOpphold(OppholdVurdering.UAVKLART))
                    )
                )
            )

            val resultat = hentOppholdSomSkalVurderes(oppholdInfo, behovPerioder, emptyList())

            // Begge opphold er sammenhengende og slås sammen til ett DTO, som dekker hele kjedens periode
            assertThat(resultat).hasSize(1)
            assertThat(resultat.first().oppholdFra).isEqualTo(4 november 2025)
            assertThat(resultat.first().avsluttetDato).isEqualTo(1 juli 2026)
        }

        // -------------------------------------------------------------------------
        // Ingen opphold / ingen behov
        // -------------------------------------------------------------------------

        @Test
        fun `ingen opphold og ingen behov gir tom liste`() {
            val resultat = hentOppholdSomSkalVurderes(
                oppholdInfo = Tidslinje(),
                behovPerioder = Tidslinje(),
                vedtatteVurderingerDto = emptyList()
            )
            assertThat(resultat).isEmpty()
        }

        @Test
        fun `opphold finnes men ingen behov og ingen vedtatte vurderinger gir tom liste`() {
            val oppholdFom = 1 januar 2026
            val oppholdTom = 31 desember 2026
            val oppholdInfo = oppholdTidslinje(Periode(oppholdFom, oppholdTom) to sykehusA)

            val resultat = hentOppholdSomSkalVurderes(
                oppholdInfo = oppholdInfo,
                behovPerioder = Tidslinje(),
                vedtatteVurderingerDto = emptyList()
            )
            assertThat(resultat).isEmpty()
        }

        // -------------------------------------------------------------------------
        // Ett opphold — behovperiode hører til oppholdet
        // -------------------------------------------------------------------------

        @Test
        fun `ett opphold med behovperiode innenfor oppholdet inkluderes`() {
            val oppholdFom = 1 januar 2026
            val oppholdTom = 31 desember 2026
            val oppholdInfo = oppholdTidslinje(Periode(oppholdFom, oppholdTom) to sykehusA)
            val behovPerioder = behovTidslinje(Periode(1 mai 2026, 31 august 2026))

            val resultat = hentOppholdSomSkalVurderes(oppholdInfo, behovPerioder, emptyList())

            assertThat(resultat).hasSize(1)
            assertThat(resultat.first().oppholdId).isEqualTo(lagOppholdId(sykehusA.navn, oppholdFom))
        }

        @Test
        fun `ett opphold med behovperiode som dekker hele oppholdet inkluderes`() {
            val oppholdFom = 1 januar 2026
            val oppholdTom = 31 desember 2026
            val oppholdInfo = oppholdTidslinje(Periode(oppholdFom, oppholdTom) to sykehusA)
            val behovPerioder = behovTidslinje(Periode(oppholdFom, oppholdTom))

            val resultat = hentOppholdSomSkalVurderes(oppholdInfo, behovPerioder, emptyList())

            assertThat(resultat).hasSize(1)
            assertThat(resultat.first().oppholdFra).isEqualTo(oppholdFom)
            assertThat(resultat.first().avsluttetDato).isEqualTo(oppholdTom)
        }

        @Test
        fun `behovperiode utenfor oppholdet gir tom liste`() {
            val oppholdInfo = oppholdTidslinje(
                Periode(1 januar 2026, 30 juni 2026) to sykehusA
            )
            // Behovperiode starter etter oppholdet slutter
            val behovPerioder = behovTidslinje(Periode(1 august 2026, 31 desember 2026))

            val resultat = hentOppholdSomSkalVurderes(oppholdInfo, behovPerioder, emptyList())

            assertThat(resultat).isEmpty()
        }

        // -------------------------------------------------------------------------
        // To opphold — behovperioder for hvert opphold
        // -------------------------------------------------------------------------

        @Test
        fun `to opphold med behovperiode for hvert opphold — begge inkluderes`() {
            val oppholdFomA = 1 januar 2026
            val oppholdTomA = 30 juni 2026
            val oppholdFomB = 1 august 2026
            val oppholdTomB = 31 desember 2026
            val oppholdInfo = oppholdTidslinje(
                Periode(oppholdFomA, oppholdTomA) to sykehusA,
                Periode(oppholdFomB, oppholdTomB) to sykehusB
            )
            val behovPerioder = behovTidslinje(
                Periode(1 mai 2026, 30 juni 2026),
                Periode(1 august 2026, 31 oktober 2026)
            )

            val resultat = hentOppholdSomSkalVurderes(oppholdInfo, behovPerioder, emptyList())

            assertThat(resultat).hasSize(2)
            assertThat(resultat.map { it.oppholdId }).containsExactlyInAnyOrder(
                lagOppholdId(sykehusA.navn, oppholdFomA),
                lagOppholdId(sykehusB.navn, oppholdFomB)
            )
        }

        @Test
        fun `to opphold — behovperiode kun for ett av dem gir bare ett opphold`() {
            val oppholdFomA = 1 januar 2026
            val oppholdTomA = 30 juni 2026
            val oppholdFomB = 1 august 2026
            val oppholdTomB = 31 desember 2026
            val oppholdInfo = oppholdTidslinje(
                Periode(oppholdFomA, oppholdTomA) to sykehusA,
                Periode(oppholdFomB, oppholdTomB) to sykehusB
            )
            // Behovperiode kun for sykehusB
            val behovPerioder = behovTidslinje(Periode(1 september 2026, 30 november 2026))

            val resultat = hentOppholdSomSkalVurderes(oppholdInfo, behovPerioder, emptyList())

            assertThat(resultat).hasSize(1)
            assertThat(resultat.first().oppholdId).isEqualTo(lagOppholdId(sykehusB.navn, oppholdFomB))
        }

        // -------------------------------------------------------------------------
        // Behovperiode strekker seg over flere opphold
        // -------------------------------------------------------------------------

        @Test
        fun `to sammenhengende opphold med behovperiode over begge slås sammen til ett opphold`() {
            val oppholdFomA = 1 januar 2026
            val oppholdTomA = 30 juni 2026
            val oppholdFomB = 1 juli 2026 // dagen etter A slutter -> sammenhengende
            val oppholdTomB = 31 desember 2026
            val oppholdInfo = oppholdTidslinje(
                Periode(oppholdFomA, oppholdTomA) to sykehusA,
                Periode(oppholdFomB, oppholdTomB) to sykehusB
            )
            // Én behovperiode som spenner over begge opphold
            val behovPerioder = behovTidslinje(Periode(oppholdFomA, oppholdTomB))

            val resultat = hentOppholdSomSkalVurderes(oppholdInfo, behovPerioder, emptyList())

            // A og B er sammenhengende (B starter dagen etter A slutter) og slås derfor sammen til ett opphold
            assertThat(resultat).hasSize(1)
            assertThat(resultat.first().oppholdId).isEqualTo(lagOppholdId(sykehusA.navn, oppholdFomA))
            assertThat(resultat.first().oppholdFra).isEqualTo(oppholdFomA)
            assertThat(resultat.first().avsluttetDato).isEqualTo(oppholdTomB)
        }

        @Test
        fun `tre sammenhengende opphold med behovperiode over alle slås sammen til ett opphold`() {
            val oppholdFomA = 1 januar 2026
            val oppholdTomA = 30 april 2026
            val oppholdFomB = 1 mai 2026 // dagen etter A slutter
            val oppholdTomB = 31 august 2026
            val oppholdFomC = 1 september 2026 // dagen etter B slutter
            val oppholdTomC = 31 desember 2026
            val oppholdInfo = oppholdTidslinje(
                Periode(oppholdFomA, oppholdTomA) to sykehusA,
                Periode(oppholdFomB, oppholdTomB) to sykehusB,
                Periode(oppholdFomC, oppholdTomC) to sykehusC
            )
            val behovPerioder = behovTidslinje(Periode(1 mars 2026, 30 november 2026))

            val resultat = hentOppholdSomSkalVurderes(oppholdInfo, behovPerioder, emptyList())

            // Alle tre er sammenhengende og slås sammen til ett opphold
            assertThat(resultat).hasSize(1)
            assertThat(resultat.first().oppholdId).isEqualTo(lagOppholdId(sykehusA.navn, oppholdFomA))
            assertThat(resultat.first().oppholdFra).isEqualTo(oppholdFomA)
            assertThat(resultat.first().avsluttetDato).isEqualTo(oppholdTomC)
        }

        // -------------------------------------------------------------------------
        // Vedtatte vurderinger
        // -------------------------------------------------------------------------

        @Test
        fun `opphold med kun vedtatt vurdering og ingen behovperiode inkluderes`() {
            val oppholdFomA = 1 januar 2026
            val oppholdTomA = 30 juli 2026
            val oppholdInfo = oppholdTidslinje(
                Periode(oppholdFomA, oppholdTomA) to sykehusA
            )
            val vedtatteVurderinger = listOf(
                vedtattVurdering(sykehusA, oppholdFomA, Periode(1 mai 2026, 30 juni 2026))
            )

            val resultat = hentOppholdSomSkalVurderes(oppholdInfo, Tidslinje(), vedtatteVurderinger)

            assertThat(resultat).hasSize(1)
            assertThat(resultat.first().oppholdId).isEqualTo(lagOppholdId(sykehusA.navn, oppholdFomA))
        }

        @Test
        fun `to opphold — ett med behovperiode og ett med kun vedtatt vurdering — begge inkluderes`() {
            val oppholdFomA = 1 januar 2026
            val oppholdTomA = 30 juni 2026
            val oppholdFomB = 1 august 2026
            val oppholdTomB = 31 desember 2026
            val oppholdInfo = oppholdTidslinje(
                Periode(oppholdFomA, oppholdTomA) to sykehusA,
                Periode(oppholdFomB, oppholdTomB) to sykehusB
            )
            val behovPerioder = behovTidslinje(
                Periode(1 mai 2026, 30 juni 2026)
            )
            val vedtatteVurderinger = listOf(
                vedtattVurdering(sykehusB, oppholdFomB, Periode(1 august 2026, 31 oktober 2026))
            )

            val resultat = hentOppholdSomSkalVurderes(oppholdInfo, behovPerioder, vedtatteVurderinger)

            assertThat(resultat).hasSize(2)
            assertThat(resultat.map { it.oppholdId }).containsExactlyInAnyOrder(
                lagOppholdId(sykehusA.navn, oppholdFomA),
                lagOppholdId(sykehusB.navn, oppholdFomB)
            )
        }

        @Test
        fun `opphold med både behovperiode og vedtatt vurdering skal ikke dupliseres`() {
            val oppholdFomA = 1 januar 2026
            val oppholdTomA = 31 desember 2026
            val oppholdInfo = oppholdTidslinje(
                Periode(oppholdFomA, oppholdTomA) to sykehusA
            )
            val behovPerioder = behovTidslinje(
                Periode(1 mai 2026, 31 august 2026)
            )
            val vedtatteVurderinger = listOf(
                vedtattVurdering(sykehusA, oppholdFomA, Periode(1 mai 2026, 30 juni 2026))
            )

            val resultat = hentOppholdSomSkalVurderes(oppholdInfo, behovPerioder, vedtatteVurderinger)

            assertThat(resultat).hasSize(1)
            assertThat(resultat.first().oppholdId).isEqualTo(lagOppholdId(sykehusA.navn, oppholdFomA))
        }

        @Test
        fun `vedtatt vurdering med oppholdId som ikke matcher noe opphold i oppholdInfo ignoreres`() {
            val oppholdFomA = 1 januar 2026
            val oppholdInfo = oppholdTidslinje(
                Periode(oppholdFomA, 30 juni 2026) to sykehusA
            )
            val vedtatteVurderinger = listOf(
                HelseoppholdDto(
                    periode = Periode(1 januar 2026, 31 mars 2026),
                    oppholdId = lagOppholdId("Ukjent sykehus", 1 januar 2025),
                    vurderinger = emptyList(),
                    status = OppholdVurderingDto.GODKJENT
                )
            )

            val resultat = hentOppholdSomSkalVurderes(oppholdInfo, Tidslinje(), vedtatteVurderinger)

            assertThat(resultat).isEmpty()
        }

        @Test
        fun `tre opphold - kombinasjon av behovperioder og vedtatte vurderinger, hvor A og B er sammenhengende`() {
            val oppholdFomA = 1 januar 2026
            val oppholdTomA = 30 april 2026
            val oppholdFomB = 1 mai 2026 // dagen etter A slutter -> sammenhengende med A
            val oppholdTomB = 31 august 2026
            val oppholdFomC = 1 oktober 2026 // reelt gap fra B (ikke sammenhengende)
            val oppholdTomC = 31 desember 2026
            val oppholdInfo = oppholdTidslinje(
                Periode(oppholdFomA, oppholdTomA) to sykehusA,
                Periode(oppholdFomB, oppholdTomB) to sykehusB,
                Periode(oppholdFomC, oppholdTomC) to sykehusC
            )
            // Behovperiode dekker den sammenhengende kjeden A+B
            val behovPerioder = behovTidslinje(
                Periode(1 mars 2026, 31 juli 2026),
            )
            // Vedtatt vurdering kun for sykehusC, som står alene (ikke sammenhengende med B)
            val vedtatteVurderinger = listOf(
                vedtattVurdering(sykehusC, oppholdFomC, Periode(1 oktober 2026, 30 november 2026))
            )

            val resultat = hentOppholdSomSkalVurderes(oppholdInfo, behovPerioder, vedtatteVurderinger)

            // A+B slås sammen til ett opphold, C forblir separat -> to opphold totalt
            assertThat(resultat).hasSize(2)
            assertThat(resultat.map { it.oppholdId }).containsExactlyInAnyOrder(
                lagOppholdId(sykehusA.navn, oppholdFomA),
                lagOppholdId(sykehusC.navn, oppholdFomC)
            )
        }

        // -------------------------------------------------------------------------
        // Konsistent oppholdId mellom hentOppholdSomSkalVurderes og mapVurderingerToDto.
        // For sammenhengende kjeder av opphold skal det genereres ett oppholdId som er konsistent mellom de to funksjonene.
        // -------------------------------------------------------------------------
        @Nested
        @DisplayName("Tester at oppholdId er konsistent mellom opphold-DTO og vurdering-DTO for sammenhengende kjeder")
        inner class KonsistentOppholdIdForSammenhengendeKjede {
            private val sykehus1 = Institusjon(Institusjonstype.HS, Oppholdstype.H, "111000111", "Sykehus 1")
            private val sykehus2 = Institusjon(Institusjonstype.HS, Oppholdstype.D, "222000222", "Sykehus 2")

            private fun oppholdTidslinje(vararg segmenter: Pair<Periode, Institusjon>): Tidslinje<Institusjon> =
                segmenter.asList().somTidslinje({ it.first }, { it.second })

            private fun behovTidslinje(vararg perioder: Periode): Tidslinje<InstitusjonsoppholdVurdering> =
                Tidslinje(perioder.map { periode ->
                    Segment(periode, InstitusjonsoppholdVurdering(helse = HelseOpphold(OppholdVurdering.UAVKLART)))
                })

            @Test
            fun `oppholdId fra hentOppholdSomSkalVurderes matcher oppholdId fra mapVurderingerToDto for sammenhengende kjede`() {
                val oppholdFom1 = 4 november 2025
                val oppholdTom1 = 7 januar 2026
                val oppholdFom2 = 8 januar 2026
                val oppholdTom2 = 1 juli 2026

                val oppholdInfo = oppholdTidslinje(
                    Periode(oppholdFom1, oppholdTom1) to sykehus1,
                    Periode(oppholdFom2, oppholdTom2) to sykehus2
                )

                val behovPerioder = behovTidslinje(Periode(oppholdFom1, oppholdTom2))

                val oppholdDto = hentOppholdSomSkalVurderes(oppholdInfo, behovPerioder, emptyList())
                assertThat(oppholdDto).hasSize(1)
                val forventetOppholdId = lagOppholdId(sykehus1.navn, oppholdFom1)
                assertThat(oppholdDto.first().oppholdId).isEqualTo(forventetOppholdId)

                val vurderingerPerOpphold = mapOf(
                    Periode(oppholdFom1, oppholdTom2) to listOf(
                        HelseinstitusjonVurdering(
                            begrunnelse = "Reduksjon over hele kjeden",
                            faarFriKostOgLosji = true,
                            forsoergerEktefelle = false,
                            harFasteUtgifter = false,
                            periode = Periode(oppholdFom1, oppholdTom2),
                            vurdertIBehandling = BehandlingId(1L),
                            vurdertAv = null,
                            vurdertTidspunkt = null
                        )
                    )
                )

                val vurderingDto = mapVurderingerToDto(
                    vurderingerPerOpphold,
                    oppholdInfo,
                    vurdertAvService = mockk(relaxed = true)
                )

                assertThat(vurderingDto).hasSize(1)
                assertThat(vurderingDto.first().oppholdId).isEqualTo(forventetOppholdId)
                assertThat(vurderingDto.first().oppholdId).isEqualTo(oppholdDto.first().oppholdId)
            }

            @Test
            fun `oppholdId matcher konsistent selv om kjeden har tre sammenhengende opphold`() {
                val sykehus3 = Institusjon(Institusjonstype.HS, Oppholdstype.H, "333000333", "Sykehus 3")

                val oppholdFom1 = 1 januar 2026
                val oppholdTom1 = 15 januar 2026
                val oppholdFom2 = 15 januar 2026
                val oppholdTom2 = 15 februar 2026
                val oppholdFom3 = 15 februar 2026
                val oppholdTom3 = 1 mai 2026

                val oppholdInfo = oppholdTidslinje(
                    Periode(oppholdFom1, oppholdTom1) to sykehus1,
                    Periode(oppholdFom2, oppholdTom2) to sykehus2,
                    Periode(oppholdFom3, oppholdTom3) to sykehus3
                )

                val behovPerioder = behovTidslinje(Periode(oppholdFom1, oppholdTom3))

                val oppholdDto = hentOppholdSomSkalVurderes(oppholdInfo, behovPerioder, emptyList())
                assertThat(oppholdDto).hasSize(1)
                val forventetOppholdId = lagOppholdId(sykehus1.navn, oppholdFom1)
                assertThat(oppholdDto.first().oppholdId).isEqualTo(forventetOppholdId)

                val vurderingerPerOpphold = mapOf(
                    Periode(oppholdFom1, oppholdTom3) to listOf(
                        HelseinstitusjonVurdering(
                            begrunnelse = "Reduksjon over hele trippel-kjeden",
                            faarFriKostOgLosji = true,
                            forsoergerEktefelle = false,
                            harFasteUtgifter = false,
                            periode = Periode(oppholdFom1, oppholdTom3),
                            vurdertIBehandling = BehandlingId(1L),
                            vurdertAv = null,
                            vurdertTidspunkt = null
                        )
                    )
                )

                val vurderingDto = mapVurderingerToDto(
                    vurderingerPerOpphold,
                    oppholdInfo,
                    vurdertAvService = mockk(relaxed = true)
                )

                assertThat(vurderingDto).hasSize(1)
                assertThat(vurderingDto.first().oppholdId).isEqualTo(forventetOppholdId)
                assertThat(vurderingDto.first().oppholdId).isEqualTo(oppholdDto.first().oppholdId)
            }
        }

        // -------------------------------------------------------------------------
        // Flere opphold med reelt gap (IKKE sammenhengende) — skal forbli separate
        // -------------------------------------------------------------------------

        @Test
        fun `to opphold med reelt gap forblir separate selv når én behovperiode dekker begge perioder med gap`() {
            val oppholdFomA = 1 januar 2026
            val oppholdTomA = 30 juni 2026
            val oppholdFomB = 5 juli 2026 // flere dagers gap fra A -> IKKE sammenhengende
            val oppholdTomB = 31 desember 2026
            val oppholdInfo = oppholdTidslinje(
                Periode(oppholdFomA, oppholdTomA) to sykehusA,
                Periode(oppholdFomB, oppholdTomB) to sykehusB
            )
            // Behovperiodene treffer hvert opphold for seg (siden det er et reelt gap mellom dem
            // vil ikke behov.perioderTilVurdering normalt inneholde selve gapet, men vi tester her at
            // hentOppholdSomSkalVurderes uansett IKKE slår sammen opphold med reelt gap)
            val behovPerioder = behovTidslinje(
                Periode(oppholdFomA, oppholdTomA),
                Periode(oppholdFomB, oppholdTomB)
            )

            val resultat = hentOppholdSomSkalVurderes(oppholdInfo, behovPerioder, emptyList())

            assertThat(resultat).hasSize(2)
            assertThat(resultat.map { it.oppholdId }).containsExactlyInAnyOrder(
                lagOppholdId(sykehusA.navn, oppholdFomA),
                lagOppholdId(sykehusB.navn, oppholdFomB)
            )
        }

        @Test
        fun `tre opphold med reelt gap mellom alle forblir tre separate opphold`() {
            val oppholdFomA = 1 januar 2026
            val oppholdTomA = 28 februar 2026
            val oppholdFomB = 1 april 2026 // gap på en måned fra A
            val oppholdTomB = 31 mai 2026
            val oppholdFomC = 1 august 2026 // gap på to måneder fra B
            val oppholdTomC = 30 september 2026
            val oppholdInfo = oppholdTidslinje(
                Periode(oppholdFomA, oppholdTomA) to sykehusA,
                Periode(oppholdFomB, oppholdTomB) to sykehusB,
                Periode(oppholdFomC, oppholdTomC) to sykehusC
            )
            val behovPerioder = behovTidslinje(
                Periode(oppholdFomA, oppholdTomA),
                Periode(oppholdFomB, oppholdTomB),
                Periode(oppholdFomC, oppholdTomC)
            )

            val resultat = hentOppholdSomSkalVurderes(oppholdInfo, behovPerioder, emptyList())

            assertThat(resultat).hasSize(3)
            assertThat(resultat.map { it.oppholdId }).containsExactlyInAnyOrder(
                lagOppholdId(sykehusA.navn, oppholdFomA),
                lagOppholdId(sykehusB.navn, oppholdFomB),
                lagOppholdId(sykehusC.navn, oppholdFomC)
            )
        }

        // -------------------------------------------------------------------------
        // Grensetilfeller for hva som regnes som "sammenhengende"
        // -------------------------------------------------------------------------

        @Test
        fun `opphold der neste starter dagen etter forrige slutter regnes som sammenhengende og slås sammen`() {
            val oppholdFomA = 1 januar 2026
            val oppholdTomA = 31 januar 2026
            val oppholdFomB = 1 februar 2026 // dagen etter A slutter -> sammenhengende (null dagers gap)
            val oppholdTomB = 28 februar 2026
            val oppholdInfo = oppholdTidslinje(
                Periode(oppholdFomA, oppholdTomA) to sykehusA,
                Periode(oppholdFomB, oppholdTomB) to sykehusB
            )
            val behovPerioder = behovTidslinje(Periode(oppholdFomA, oppholdTomB))

            val resultat = hentOppholdSomSkalVurderes(oppholdInfo, behovPerioder, emptyList())

            assertThat(resultat).hasSize(1)
            assertThat(resultat.first().oppholdFra).isEqualTo(oppholdFomA)
            assertThat(resultat.first().avsluttetDato).isEqualTo(oppholdTomB)
        }

        @Test
        fun `opphold med minst én dags reelt gap regnes IKKE som sammenhengende og forblir separate`() {
            val oppholdFomA = 1 januar 2026
            val oppholdTomA = 31 januar 2026
            val oppholdFomB = 2 februar 2026 // én dags gap (1. feb) -> IKKE sammenhengende
            val oppholdTomB = 28 februar 2026
            val oppholdInfo = oppholdTidslinje(
                Periode(oppholdFomA, oppholdTomA) to sykehusA,
                Periode(oppholdFomB, oppholdTomB) to sykehusB
            )
            val behovPerioder = behovTidslinje(
                Periode(oppholdFomA, oppholdTomA),
                Periode(oppholdFomB, oppholdTomB)
            )

            val resultat = hentOppholdSomSkalVurderes(oppholdInfo, behovPerioder, emptyList())

            assertThat(resultat).hasSize(2)
            assertThat(resultat.map { it.oppholdId }).containsExactlyInAnyOrder(
                lagOppholdId(sykehusA.navn, oppholdFomA),
                lagOppholdId(sykehusB.navn, oppholdFomB)
            )
        }

        @Test
        fun `opphold med to dagers gap regnes IKKE som sammenhengende og forblir separate`() {
            val oppholdFomA = 1 januar 2026
            val oppholdTomA = 31 januar 2026
            val oppholdFomB = 3 februar 2026 // to dagers gap (1. og 2. feb) -> IKKE sammenhengende
            val oppholdTomB = 28 februar 2026
            val oppholdInfo = oppholdTidslinje(
                Periode(oppholdFomA, oppholdTomA) to sykehusA,
                Periode(oppholdFomB, oppholdTomB) to sykehusB
            )
            val behovPerioder = behovTidslinje(
                Periode(oppholdFomA, oppholdTomA),
                Periode(oppholdFomB, oppholdTomB)
            )

            val resultat = hentOppholdSomSkalVurderes(oppholdInfo, behovPerioder, emptyList())

            assertThat(resultat).hasSize(2)
            assertThat(resultat.map { it.oppholdId }).containsExactlyInAnyOrder(
                lagOppholdId(sykehusA.navn, oppholdFomA),
                lagOppholdId(sykehusB.navn, oppholdFomB)
            )
        }

        @Test
        fun `vedtatt vurdering som refererer til et senere opphold i en sammenhengende kjede bør fortsatt matches`() {
            val oppholdFomA = 1 januar 2026
            val oppholdTomA = 30 april 2026
            val oppholdFomB = 1 mai 2026 // dagen etter A slutter -> sammenhengende med A
            val oppholdTomB = 31 august 2026
            val oppholdInfo = oppholdTidslinje(
                Periode(oppholdFomA, oppholdTomA) to sykehusA,
                Periode(oppholdFomB, oppholdTomB) to sykehusB
            )
            // Vedtatt vurdering peker til sykehusB (det andre segmentet i kjeden), ikke kjedens "offisielle" oppholdId
            val vedtatteVurderinger = listOf(
                vedtattVurdering(sykehusB, oppholdFomB, Periode(1 mai 2026, 30 juni 2026))
            )

            val resultat = hentOppholdSomSkalVurderes(oppholdInfo, Tidslinje(), vedtatteVurderinger)

            // Kjeden (A+B) skal fortsatt vises som ett opphold med oppholdId basert på A (kjedens første segment),
            // selv om den vedtatte vurderingen opprinnelig pekte til B sin oppholdId
            assertThat(resultat).hasSize(1)
            assertThat(resultat.first().oppholdId).isEqualTo(lagOppholdId(sykehusA.navn, oppholdFomA))
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
