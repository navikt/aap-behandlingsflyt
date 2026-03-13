package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Utfall.REDUKSJON
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Utfall.UNNTAK
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.FRAVÆR_FØRSTE_DAG_I_MELDEPERIODE
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.FraværForPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.FraværÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class FraværFastsattAktivitetRegelTest {

    @Test
    fun `ingen brudd, ingen vurderinger`() {
        val vurderinger = lagFraværVurderingTidslinje(
            rettighetsperiode = 2020 tilOgMed 2022
        )
        assertEquals(0, vurderinger.segmenter().count())
    }

    @Test
    fun `ett fravær fra tiltak, 11-8 kan ikke brukes`() {
        val vurderinger = lagFraværVurderingTidslinje(
            rettighetsperiode = 2020 tilOgMed 2022,
            fraværForPeriode(
                1 januar 2020
            ),
        )
        assertEquals(1, vurderinger.segmenter().count())
        val vurdering = vurderinger.segment(1 januar 2020)!!.verdi
        assertEquals(FRAVÆR_FØRSTE_DAG_I_MELDEPERIODE, vurdering.vilkårsvurdering)
    }

    @Test
    fun `andre dag i meldeperiode fører til § 11-8 reduksjon`() {
        val vurdering = lagFraværVurderingTidslinje(
            rettighetsperiode = 2020 tilOgMed 2022,
            fraværForPeriode(
                1 januar 2020
            ),
            fraværForPeriode(
                2 januar 2020
            ),
        )
        // TODO : Burde begge asserts forvente reduksjon for 2 påfølgende dager ? Noe ala dette her?
        //assertEquals(REDUKSJON, vurdering.segment(LocalDate.of(2020, 1, 1))!!.verdi.vilkårsvurdering)
        //assertEquals(REDUKSJON, vurdering.segment(LocalDate.of(2020, 1, 2))!!.verdi.vilkårsvurdering)

        assertEquals(REDUKSJON, vurdering.segment(1 januar 2020)!!.verdi.utfall)
        assertEquals(REDUKSJON, vurdering.segment(2 januar 2020)!!.verdi.utfall)
    }

    @Test
    fun `første dag med velferdsgrunner, andre og tredje dag uten grunn fører til § 11-8 reduksjon`() {
        val vurdering = lagFraværVurderingTidslinje(
            rettighetsperiode = 2020 tilOgMed 2022,
            fraværForPeriode(
                1 januar 2020,
                fraværÅrsak = FraværÅrsak.OMSORG_ANNEN_STERK_GRUNN,
            ),
            fraværForPeriode(
                2 januar 2020,
                fraværÅrsak = FraværÅrsak.ANNET,
            ),
            fraværForPeriode(
                3 januar 2020,
                fraværÅrsak = FraværÅrsak.ANNET,
            ),
        )
        assertEquals(UNNTAK, vurdering.segment(1 januar 2020)!!.verdi.utfall)
        assertEquals(REDUKSJON, vurdering.segment(2 januar 2020)!!.verdi.utfall)
        assertEquals(REDUKSJON, vurdering.segment(3 januar 2020)!!.verdi.utfall)

        //Sjekker at samme segment hentes for to forskjellige datoer for å bevise at segmentene er sammenslått
        assertEquals(vurdering.segment(2 januar 2020), vurdering.segment(3 januar 2020))
    }

    @Test
    fun `første dag med sykdom, andre og tredje uten grunn, gir at andre og tredje dag fører til § 11-8 redusjon`() {
        val vurdering = lagFraværVurderingTidslinje(
            rettighetsperiode = 2020 tilOgMed 2022,
            fraværForPeriode(
                1 januar 2020,
                fraværÅrsak = FraværÅrsak.SYKDOM_ELLER_SKADE,
            ),
            fraværForPeriode(
                2 januar 2020,
                fraværÅrsak = FraværÅrsak.ANNET,
            ),
            fraværForPeriode(
                3 januar 2020,
                fraværÅrsak = FraværÅrsak.ANNET,
            ),
        )
        assertEquals(UNNTAK, vurdering.segment(1 januar 2020)!!.verdi.utfall)
        assertEquals(REDUKSJON, vurdering.segment(2 januar 2020)!!.verdi.utfall)
        assertEquals(REDUKSJON, vurdering.segment(3 januar 2020)!!.verdi.utfall)
    }

    @Test
    fun `to fravær i hver sin meldeperiode fører ikke til § 11-8 reduksjon`() {
        val vurdering = lagFraværVurderingTidslinje(
            rettighetsperiode = 2020 tilOgMed 2022,
            fraværForPeriode(
                1 januar 2020
            ),
            fraværForPeriode(
                15 januar 2020
            ),
        )
        assertEquals(UNNTAK, vurdering.segment(1 januar 2020)!!.verdi.utfall)
        assertEquals(UNNTAK, vurdering.segment(15 januar 2020)!!.verdi.utfall)
    }

    @Test
    fun `11 dager med gydlig fravær i en meldeperiode skal gi § 11-8 reduksjon fra siste fraværsdag`() {
        val vurderinger = lagFraværVurderingTidslinje(
            rettighetsperiode = 2020 tilOgMed 2022,
            Periode(1 januar 2020, 12 januar 2020).dager().map { dato ->
                fraværForPeriode(
                    dato,
                    fraværÅrsak = FraværÅrsak.OMSORG_ANNEN_STERK_GRUNN,
                )
            }
        )

        /* Fravær dagene 1 – 10: gir ikke stans pga. gyldig grunn, men bruker opp 10 dager av kvoten for kalenderåret. */
        for (dag in 1..10) {
            val muligeUtfall = vurderinger.segment(dag januar 2020)!!.verdi.utfall
            assertEquals(UNNTAK, muligeUtfall)
        }

        /* Fravær dag 11: gyldig grunn, men gir stans fordi kvoten er brukt opp. */
        vurderinger.segment(11 januar 2020)!!.verdi.also {
            assertEquals(REDUKSJON, it.utfall)
        }
    }

    @Test
    fun `10-dagers-kvote telles ikke under 11-7-stans`() {
        // TODO skal ikke ta høyde for 11-7 i underveis?
    }

    @Test
    fun `10-dagers-kvote starter på 0 i nytt kalenderår`() {
        val startMeldeperiode2021 = 13
        val vurderinger = lagFraværVurderingTidslinje(
            rettighetsperiode = 2020 tilOgMed 2022,
            /* Fem brudd det første året (2020), hvorav fire teller mot kvoten. */
            fraværForPeriode(
                Periode(fom = 1 januar 2020, tom = 5 januar 2020),
                fraværÅrsak = FraværÅrsak.OMSORG_ANNEN_STERK_GRUNN,
            ),
            fraværForPeriode(
                Periode(
                    fom = startMeldeperiode2021 januar 2021,
                    tom = (startMeldeperiode2021 + 11) januar 2021,
                ),
                fraværÅrsak = FraværÅrsak.OMSORG_ANNEN_STERK_GRUNN,
            )
        )

        /* Første 10 fravær i 2021 gir ikke stans pga ti dager fravær per kalenderår. */
        for (dag in startMeldeperiode2021..<startMeldeperiode2021 + 10) {
            val kanStanses = vurderinger.segment(dag januar 2021)!!.verdi.utfall
            assertEquals(UNNTAK, kanStanses)
        }

        /* Fravær dag 11: gyldig grunn, men gir stans fordi kvoten er brukt opp. */
        vurderinger.segment((startMeldeperiode2021 + 10) januar 2021)!!.verdi.also {
            assertEquals(REDUKSJON, it.utfall)
        }
    }

    /**
     * 2020
     *       January
     *  Mo Tu We Th Fr Sa Su
     *         1  2  3  4  5
     *   6  7  8  9 10 11 12
     *  13 14 15 16 17 18 19
     *  20 21 22 23 24 25 26
     *  27 28 29 30 31
     *
     */
    @Test
    fun `brudd som strekker seg over to meldeperioder blir vurdert i hver sin meldeperiode`() {
        val vurderinger = lagFraværVurderingTidslinje(
            rettighetsperiode = Periode(fom = 17 januar 2020, tom = 31 desember 2022),
            fraværForPeriode(
                26 januar 2020
            ),
            fraværForPeriode(
                27 januar 2020,
            ),
        )

        vurderinger.segment(26 januar 2020)!!.verdi.also {
            assertEquals(UNNTAK, it.utfall)
        }
        vurderinger.segment(27 januar 2020)!!.verdi.also {
            assertEquals(UNNTAK, it.utfall)
        }
    }

    @Test
    fun `to brudd inntil grensen for en meldeperiode blir registrert i samme meldeperiode`() {
        val vurderinger = lagFraværVurderingTidslinje(
            rettighetsperiode = 2020 tilOgMed 2022,
            fraværForPeriode(
                13 januar 2020,
            ),
            fraværForPeriode(
                14 januar 2020,
            ),
        )

        vurderinger.segment(13 januar 2020)!!.verdi.also {
            assertEquals(REDUKSJON, it.utfall)
        }
        vurderinger.segment(14 januar 2020)!!.verdi.also {
            assertEquals(REDUKSJON, it.utfall)
        }
    }

    private fun lagFraværVurderingTidslinje(
        rettighetsperiode: Periode,
        startTidslinje: Tidslinje<Vurdering>,
        vararg fraværForPeriode: FraværForPeriode,
    ): Tidslinje<FraværFastsattAktivitetVurdering> {

        // TODO mer fornuftige perioder
        val meldekort = fraværForPeriode.mapIndexed { index, fraværForDag ->
            Meldekort(
                journalpostId = JournalpostId(index.toString()),
                timerArbeidPerPeriode = emptySet(),
                mottattTidspunkt = LocalDateTime.now().plusMinutes(index.toLong()),
                fravær = setOf(fraværForDag)
            )
        }
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            meldekort = meldekort,
        )
        return FraværFastsattAktivitetRegel().vurder(input, UtledMeldeperiodeRegel().vurder(input, startTidslinje))
            .filter { it.verdi.fraværFastsattAktivitetVurdering != null }
            .mapValue { it.fraværFastsattAktivitetVurdering!! }
    }

    private fun lagFraværVurderingTidslinje(
        rettighetsperiode: Periode,
        vararg aktivitetspliktDokument: FraværForPeriode,
    ): Tidslinje<FraværFastsattAktivitetVurdering> {
        return lagFraværVurderingTidslinje(rettighetsperiode, Tidslinje(), *aktivitetspliktDokument)
    }

    private fun lagFraværVurderingTidslinje(
        rettighetsperiode: Periode,
        aktivitetspliktDokument: List<FraværForPeriode>,
    ): Tidslinje<FraværFastsattAktivitetVurdering> {
        return lagFraværVurderingTidslinje(rettighetsperiode, Tidslinje(), *aktivitetspliktDokument.toTypedArray())
    }

}

fun fraværForPeriode(
    tilOgFra: LocalDate,
    fraværÅrsak: FraværÅrsak = FraværÅrsak.ANNET,
): FraværForPeriode = fraværForPeriode(
    periode = Periode(tilOgFra, tilOgFra),
    fraværÅrsak = fraværÅrsak,
)

fun fraværForPeriode(
    periode: Periode,
    fraværÅrsak: FraværÅrsak = FraværÅrsak.ANNET,
): FraværForPeriode {
    return FraværForPeriode(
        periode = periode,
        fraværÅrsak = fraværÅrsak
    )
}

infix fun Int.tilOgMed(tilÅr: Int): Periode = Periode(1 januar this, 31 desember tilÅr)
