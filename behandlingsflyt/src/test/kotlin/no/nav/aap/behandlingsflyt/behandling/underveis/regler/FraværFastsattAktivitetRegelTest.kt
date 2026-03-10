package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.STANS_ANDRE_DAG
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.STANS_TI_DAGER_BRUKT_OPP
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_INNTIL_EN_DAG
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_STERKE_VELFERDSGRUNNER
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_SYKDOM_ELLER_SKADE
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.FraværForDag
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
import kotlin.collections.toTypedArray

class FraværFastsattAktivitetRegelTest {
    @Test
    fun `ingen brudd, ingen vurderinger`() {
        val vurderinger = aktivitetsbruddVurderinger(
            rettighetsperiode = Periode(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2022, 12, 31))
        )
        assertEquals(0, vurderinger.segmenter().count())
    }

    @Test
    fun `ett fravær fra tiltak, 11-8 kan ikke brukes`() {
        val vurderinger = aktivitetsbruddVurderinger(
            rettighetsperiode = Periode(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2022, 12, 31)),
            brudd(
                dato = LocalDate.of(2020, 1, 1),
            ),
        )
        assertEquals(1, vurderinger.segmenter().count())
        val vurdering = vurderinger.segment(LocalDate.of(2020, 1, 1))!!.verdi
        assertEquals(UNNTAK_INNTIL_EN_DAG, vurdering.vilkårsvurdering)
    }

    @Test
    fun `andre dag i meldeperiode kan føre til § 11-8 stans`() {
        val vurdering = aktivitetsbruddVurderinger(
            rettighetsperiode = Periode(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2022, 12, 31)),
            brudd(
                dato = LocalDate.of(2020, 1, 1),
            ),
            brudd(
                dato = LocalDate.of(2020, 1, 2),
            ),
        )
        assertEquals(UNNTAK_INNTIL_EN_DAG, vurdering.segment(LocalDate.of(2020, 1, 1))!!.verdi.vilkårsvurdering)
        assertEquals(STANS_ANDRE_DAG, vurdering.segment(LocalDate.of(2020, 1, 2))!!.verdi.vilkårsvurdering)
    }

    @Test
    fun `første dag med velferdsgrunner, andre dag uten grunn fører til § 11-8 stans`() {
        val vurdering = aktivitetsbruddVurderinger(
            rettighetsperiode = Periode(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2022, 12, 31)),
            brudd(
                dato = LocalDate.of(2020, 1, 1),
                fraværÅrsak = FraværÅrsak.OMSORG_ANNEN_STERK_GRUNN,
            ),
            brudd(
                dato = LocalDate.of(2020, 1, 2),
                fraværÅrsak = FraværÅrsak.ANNET,
            ),
            brudd(
                dato = LocalDate.of(2020, 1, 3),
                fraværÅrsak = FraværÅrsak.ANNET,
            ),
        )
        assertEquals(
            UNNTAK_STERKE_VELFERDSGRUNNER,
            vurdering.segment(LocalDate.of(2020, 1, 1))!!.verdi.vilkårsvurdering
        )
        assertEquals(UNNTAK_INNTIL_EN_DAG, vurdering.segment(LocalDate.of(2020, 1, 2))!!.verdi.vilkårsvurdering)
        assertEquals(STANS_ANDRE_DAG, vurdering.segment(LocalDate.of(2020, 1, 3))!!.verdi.vilkårsvurdering)
    }

    @Test
    fun `første dag med sykdom, andre og tredje uten grunn, gir stans fra tredje`() {
        val vurdering = aktivitetsbruddVurderinger(
            rettighetsperiode = Periode(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2022, 12, 31)),
            brudd(
                dato = LocalDate.of(2020, 1, 1),
                fraværÅrsak = FraværÅrsak.SYKDOM_ELLER_SKADE,
            ),
            brudd(
                dato = LocalDate.of(2020, 1, 2),
                fraværÅrsak = FraværÅrsak.ANNET,
            ),
            brudd(
                dato = LocalDate.of(2020, 1, 3),
                fraværÅrsak = FraværÅrsak.ANNET,
            ),
        )
        assertEquals(UNNTAK_SYKDOM_ELLER_SKADE, vurdering.segment(LocalDate.of(2020, 1, 1))!!.verdi.vilkårsvurdering)
        assertEquals(UNNTAK_INNTIL_EN_DAG, vurdering.segment(LocalDate.of(2020, 1, 2))!!.verdi.vilkårsvurdering)
        assertEquals(STANS_ANDRE_DAG, vurdering.segment(LocalDate.of(2020, 1, 3))!!.verdi.vilkårsvurdering)
    }

    @Test
    fun `to fravær i hver sin meldeperiode fører ikke til § 11-8 stans`() {
        val vurdering = aktivitetsbruddVurderinger(
            rettighetsperiode = Periode(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2022, 12, 31)),
            brudd(
                dato = LocalDate.of(2020, 1, 1),
            ),
            brudd(
                dato = LocalDate.of(2020, 1, 15),
            ),
        )
        assertEquals(UNNTAK_INNTIL_EN_DAG, vurdering.segment(LocalDate.of(2020, 1, 1))!!.verdi.vilkårsvurdering)
        assertEquals(UNNTAK_INNTIL_EN_DAG, vurdering.segment(LocalDate.of(2020, 1, 15))!!.verdi.vilkårsvurdering)
    }

    @Test
    fun `11 dager med gydlig fravær i en meldeperiode kan gi stans fra siste fraværsdag`() {
        val vurderinger = aktivitetsbruddVurderinger(
            rettighetsperiode = Periode(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2022, 12, 31)),
            Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 12)).dager().map { dato ->
                brudd(
                    dato = dato,
                    fraværÅrsak = FraværÅrsak.OMSORG_ANNEN_STERK_GRUNN,
                )
            }
        )

        /* Fravær dagene 1 – 10: gir ikke stans pga. gyldig grunn, men bruker opp 10 dager av kvoten for kalenderåret. */
        for (dag in 1..10) {
            val muligeUtfall = vurderinger.segment(LocalDate.of(2020, 1, dag))!!.verdi.vilkårsvurdering
            assertEquals(UNNTAK_STERKE_VELFERDSGRUNNER, muligeUtfall)
        }

        /* Fravær dag 11: gyldig grunn, men gir stans fordi kvoten er brukt opp. */
        vurderinger.segment(LocalDate.of(2020, 1, 11))!!.verdi.also {
            assertEquals(STANS_TI_DAGER_BRUKT_OPP, it.vilkårsvurdering)
        }
    }

    @Test
    fun `10-dagers-kvote telles ikke under 11-7-stans`() {
        // TODO skal ikke ta høyde for 11-7 i underveis?
    }

    @Test
    fun `10-dagers-kvote starter på 0 i nytt kalenderår`() {
        val startMeldeperiode2021 = 13
        val vurderinger = aktivitetsbruddVurderinger(
            rettighetsperiode = Periode(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2022, 12, 31)),
            /* Fem brudd det første året (2020), hvorav fire teller mot kvoten. */
            (Periode(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 1, 5)).dager() +
                    Periode(
                        fom = LocalDate.of(2021, 1, startMeldeperiode2021),
                        tom = LocalDate.of(2021, 1, startMeldeperiode2021 + 11)
                    ).dager())
                .map { dato ->
                    brudd(
                        dato = dato,
                        fraværÅrsak = FraværÅrsak.OMSORG_ANNEN_STERK_GRUNN,
                    )
                }
        )

        /* Første 10 fravær i 2021 gir ikke stans pga ti dager fravær per kalenderår. */
        for (dag in startMeldeperiode2021..<startMeldeperiode2021 + 10) {
            val kanStanses = vurderinger.segment(LocalDate.of(2021, 1, dag))!!.verdi.vilkårsvurdering
            assertEquals(UNNTAK_STERKE_VELFERDSGRUNNER, kanStanses)
        }

        /* Fravær dag 11: gyldig grunn, men gir stans fordi kvoten er brukt opp. */
        vurderinger.segment(LocalDate.of(2021, 1, startMeldeperiode2021 + 10))!!.verdi.also {
            assertEquals(STANS_TI_DAGER_BRUKT_OPP, it.vilkårsvurdering)
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
        val vurderinger = aktivitetsbruddVurderinger(
            rettighetsperiode = Periode(fom = 17 januar 2020, tom = 31 desember 2022),
            brudd(
                dato = 26 januar 2020
            ),
            brudd(
                dato = 27 januar 2020,
            ),
        )

        vurderinger.segment(26 januar 2020)!!.verdi.also {
            assertEquals(UNNTAK_INNTIL_EN_DAG, it.vilkårsvurdering)
        }
        vurderinger.segment(27 januar 2020)!!.verdi.also {
            assertEquals(UNNTAK_INNTIL_EN_DAG, it.vilkårsvurdering)
        }
    }

    @Test
    fun `to brudd inntil grensen for en meldeperiode blir registrert i samme meldeperiode`() {
        val vurderinger = aktivitetsbruddVurderinger(
            rettighetsperiode = Periode(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2022, 12, 31)),
            brudd(
                dato = LocalDate.of(2020, 1, 13),
            ),
            brudd(
                dato = LocalDate.of(2020, 1, 14),
            ),
        )

        vurderinger.segment(LocalDate.of(2020, 1, 13))!!.verdi.also {
            assertEquals(UNNTAK_INNTIL_EN_DAG, it.vilkårsvurdering)
        }
        vurderinger.segment(LocalDate.of(2020, 1, 14))!!.verdi.also {
            assertEquals(STANS_ANDRE_DAG, it.vilkårsvurdering)
        }
    }

    private fun aktivitetsbruddVurderinger(
        rettighetsperiode: Periode,
        startTidslinje: Tidslinje<Vurdering>,
        vararg aktivitetspliktDokument: FraværForDag,
    ): Tidslinje<FraværFastsattAktivitetVurdering> {

        // TODO mer fornuftige perioder
        val meldekort = aktivitetspliktDokument.mapIndexed { index, fraværForDag ->
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

    private fun aktivitetsbruddVurderinger(
        rettighetsperiode: Periode,
        vararg aktivitetspliktDokument: FraværForDag,
    ): Tidslinje<FraværFastsattAktivitetVurdering> {
        return aktivitetsbruddVurderinger(rettighetsperiode, Tidslinje(), *aktivitetspliktDokument)
    }

    private fun aktivitetsbruddVurderinger(
        rettighetsperiode: Periode,
        aktivitetspliktDokument: List<FraværForDag>,
    ): Tidslinje<FraværFastsattAktivitetVurdering> {
        return aktivitetsbruddVurderinger(rettighetsperiode, Tidslinje(), *aktivitetspliktDokument.toTypedArray())
    }
}

fun brudd(
    dato: LocalDate,
    fraværÅrsak: FraværÅrsak = FraværÅrsak.ANNET,
): FraværForDag {
    return FraværForDag(
        dato = dato,
        fraværÅrsak = fraværÅrsak
    )
}
