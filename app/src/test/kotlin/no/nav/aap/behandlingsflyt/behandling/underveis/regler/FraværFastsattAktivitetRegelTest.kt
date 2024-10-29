package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.AktivitetspliktVurdering.Vilkårsvurdering.AKTIVT_BIDRAG_IKKE_OPPFYLT
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.STANS_ANDRE_DAG
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.STANS_TI_DAGER_BRUKT_OPP
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_INNTIL_EN_DAG
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_STERKE_VELFERDSGRUNNER
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_SYKDOM_ELLER_SKADE
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_7
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_8
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_AKTIVT_BIDRAG
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_MØTT_TIL_TILTAK
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Grunn.INGEN_GYLDIG_GRUNN
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Grunn.STERKE_VELFERDSGRUNNER
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Grunn.SYKDOM_ELLER_SKADE
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class FraværFastsattAktivitetRegelTest {
    @Test
    fun `ingen brudd, ingen vurderinger`() {
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = dato(2020, 1, 1), tom = dato(2022, 12, 31))
        )
        assertEquals(0, vurderinger.segmenter().size)
    }

    @Test
    fun `ett fravær fra tiltak, 11-8 kan ikke brukes`() {
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = dato(2020, 1, 1), tom = dato(2022, 12, 31)),
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 1), dato(2020, 1, 1)),
                opprettet = dato(2020, 1, 2)
            ),
        )
        assertEquals(1, vurderinger.segmenter().size)
        val vurdering = vurderinger.segment(dato(2020, 1, 1))!!.verdi
        assertEquals(UNNTAK_INNTIL_EN_DAG, vurdering.vilkårsvurdering)
    }

    @Test
    fun `andre dag i meldeperiode kan føre til § 11-8 stans`() {
        val vurdering = vurder(
            rettighetsperiode = Periode(fom = dato(2020, 1, 1), tom = dato(2022, 12, 31)),
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 1), dato(2020, 1, 2)),
                opprettet = dato(2020, 4, 1),
            ),
        )
        assertEquals(UNNTAK_INNTIL_EN_DAG, vurdering.segment(dato(2020, 1, 1))!!.verdi.vilkårsvurdering)
        assertEquals(STANS_ANDRE_DAG, vurdering.segment(dato(2020, 1, 2))!!.verdi.vilkårsvurdering)
    }

    @Test
    fun `første dag med velferdsgrunner, andre dag uten grunn fører til § 11-8 stans`() {
        val vurdering = vurder(
            rettighetsperiode = Periode(fom = dato(2020, 1, 1), tom = dato(2022, 12, 31)),
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 1), dato(2020, 1, 1)),
                opprettet = dato(2020, 4, 1),
                grunn = STERKE_VELFERDSGRUNNER,
            ),
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 2), dato(2020, 1, 3)),
                opprettet = dato(2020, 4, 1),
                grunn = INGEN_GYLDIG_GRUNN,
            ),
        )
        assertEquals(UNNTAK_STERKE_VELFERDSGRUNNER, vurdering.segment(dato(2020, 1, 1))!!.verdi.vilkårsvurdering)
        assertEquals(UNNTAK_INNTIL_EN_DAG, vurdering.segment(dato(2020, 1, 2))!!.verdi.vilkårsvurdering)
        assertEquals(STANS_ANDRE_DAG, vurdering.segment(dato(2020, 1, 3))!!.verdi.vilkårsvurdering)
    }

    @Test
    fun `første dag med sykdom, andre og tredje uten grunn, gir stans fra tredje`() {
        val vurdering = vurder(
            rettighetsperiode = Periode(fom = dato(2020, 1, 1), tom = dato(2022, 12, 31)),
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 1), dato(2020, 1, 1)),
                opprettet = dato(2020, 4, 1),
                grunn = SYKDOM_ELLER_SKADE,
            ),
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 2), dato(2020, 1, 3)),
                opprettet = dato(2020, 4, 1),
                grunn = INGEN_GYLDIG_GRUNN,
            ),
        )
        assertEquals(UNNTAK_SYKDOM_ELLER_SKADE, vurdering.segment(dato(2020, 1, 1))!!.verdi.vilkårsvurdering)
        assertEquals(UNNTAK_INNTIL_EN_DAG, vurdering.segment(dato(2020, 1, 2))!!.verdi.vilkårsvurdering)
        assertEquals(STANS_ANDRE_DAG, vurdering.segment(dato(2020, 1, 3))!!.verdi.vilkårsvurdering)
    }

    @Test
    fun `to fravær i hver sin meldeperiode fører ikke til § 11-8 stans`() {
        val vurdering = vurder(
            rettighetsperiode = Periode(fom = dato(2020, 1, 1), tom = dato(2022, 12, 31)),
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 1), dato(2020, 1, 1)),
                opprettet = dato(2020, 4, 1),
            ),
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 15), dato(2020, 1, 15)),
                opprettet = dato(2020, 4, 1),
            ),
        )
        assertEquals(UNNTAK_INNTIL_EN_DAG, vurdering.segment(dato(2020, 1, 1))!!.verdi.vilkårsvurdering)
        assertEquals(UNNTAK_INNTIL_EN_DAG, vurdering.segment(dato(2020, 1, 15))!!.verdi.vilkårsvurdering)
    }

    @Test
    fun `11 dager med gydlig fravær i en meldeperiode kan gi stans fra siste fraværsdag`() {
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = dato(2020, 1, 1), tom = dato(2022, 12, 31)),
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 1), dato(2020, 1, 12)),
                opprettet = dato(2020, 4, 1),
                grunn = STERKE_VELFERDSGRUNNER,
            ),
        )

        /* Fravær dagene 1 – 10: gir ikke stans pga. gyldig grunn, men bruker opp 10 dager av kvoten for kalenderåret. */
        for (dag in 1..10) {
            val muligeUtfall = vurderinger.segment(dato(2020, 1, dag))!!.verdi.vilkårsvurdering
            assertEquals(UNNTAK_STERKE_VELFERDSGRUNNER, muligeUtfall)
        }

        /* Fravær dag 11: gyldig grunn, men gir stans fordi kvoten er brukt opp. */
        vurderinger.segment(dato(2020, 1, 11))!!.verdi.also {
            assertEquals(STANS_TI_DAGER_BRUKT_OPP, it.vilkårsvurdering)
        }
    }

    @Test
    fun `10-dagers-kvote telles ikke under 11-7-stans`() {
        val stansPeriode = Periode(dato(2020, 1, 4), dato(2020, 1, 4))
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = dato(2020, 1, 1), tom = dato(2022, 12, 31)),
            Tidslinje(
                stansPeriode,
                Vurdering(
                    aktivitetspliktVurdering = AktivitetspliktVurdering(
                        brudd(
                            bruddType = IKKE_AKTIVT_BIDRAG,
                            paragraf = PARAGRAF_11_7,
                            periode = stansPeriode,
                        ),
                        AKTIVT_BIDRAG_IKKE_OPPFYLT,
                    )
                )
            ),
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 1), dato(2020, 1, 13)),
                opprettet = dato(2020, 4, 1),
                grunn = STERKE_VELFERDSGRUNNER,
            ),
        )

        /* Fravær dagene 1 – 3: gir ikke stans pga. gyldig grunn, men bruker opp 10 dager av kvoten for kalenderåret. */
        for (dag in 1..3) {
            val muligeUtfall = vurderinger.segment(dato(2020, 1, dag))!!.verdi.vilkårsvurdering
            assertEquals(UNNTAK_STERKE_VELFERDSGRUNNER, muligeUtfall)
        }

        /* Fraværet dag 4 blir ikke vurdert, siden det er et 11-7-brudd som er vurdert den dagen. */
        assertNull(vurderinger.segment(dato(2020, 1, 4)))

        /* Fravær dagene 5 – 11: gir ikke stans pga. gyldig grunn, men bruker opp 10 dager av kvoten for kalenderåret. */
        for (dag in 5..11) {
            val muligeUtfall = vurderinger.segment(dato(2020, 1, dag))!!.verdi.vilkårsvurdering
            assertEquals(UNNTAK_STERKE_VELFERDSGRUNNER, muligeUtfall)
        }

        /* Fravær dag 11: gyldig grunn, men gir stans fordi kvoten er brukt opp. */
        vurderinger.segment(dato(2020, 1, 12))!!.verdi.also {
            assertEquals(STANS_TI_DAGER_BRUKT_OPP, it.vilkårsvurdering)
        }
    }

    @Test
    fun `10-dagers-kvote starter på 0 i nytt kalenderår`() {
        val startMeldeperiode2021 = 13
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = dato(2020, 1, 1), tom = dato(2022, 12, 31)),
            /* Fem brudd det første året (2020), hvorav fire teller mot kvoten. */
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 1), dato(2020, 1, 5)),
                opprettet = dato(2020, 4, 1),
                grunn = STERKE_VELFERDSGRUNNER,
            ),
            /* Tolv brudd i første meldeperiode (2021). Meldeperioden starter den 6. januar. */
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(
                    fom = dato(2021, 1, startMeldeperiode2021),
                    tom = dato(2021, 1, startMeldeperiode2021 + 11),
                ),
                opprettet = dato(2020, 4, 1),
                grunn = STERKE_VELFERDSGRUNNER,
            ),
        )

        /* Første 10 fravær i 2021 gir ikke stans pga ti dager fravær per kalenderår. */
        for (dag in startMeldeperiode2021..<startMeldeperiode2021 + 10) {
            val kanStanses = vurderinger.segment(dato(2021, 1, dag))!!.verdi.vilkårsvurdering
            assertEquals(UNNTAK_STERKE_VELFERDSGRUNNER, kanStanses)
        }

        /* Fravær dag 11: gyldig grunn, men gir stans fordi kvoten er brukt opp. */
        vurderinger.segment(dato(2021, 1, startMeldeperiode2021 + 10))!!.verdi.also {
            assertEquals(STANS_TI_DAGER_BRUKT_OPP, it.vilkårsvurdering)
        }
    }

    private fun dato(år: Int, mnd: Int, dag: Int) = LocalDate.of(år, mnd, dag)

    /* Avhengig av meldeperiodene, som settes av `MeldeplitRegel`. */
    private val meldepliktRegel = MeldepliktRegel()
    private val regel = FraværFastsattAktivitetRegel()

    private fun vurder(
        rettighetsperiode: Periode,
        startTidslinje: Tidslinje<Vurdering>,
        vararg aktivitetspliktDokument: AktivitetspliktDokument,
    ): Tidslinje<FraværFastsattAktivitetVurdering> {
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            aktivitetspliktDokument = aktivitetspliktDokument.toSet(),
        )
        return regel.vurder(input, meldepliktRegel.vurder(input, startTidslinje))
            .filter { it.verdi.fraværFastsattAktivitetVurdering != null }
            .mapValue { it.fraværFastsattAktivitetVurdering!! }
    }

    private fun vurder(
        rettighetsperiode: Periode,
        vararg aktivitetspliktDokument: AktivitetspliktDokument,
    ): Tidslinje<FraværFastsattAktivitetVurdering> {
        return vurder(rettighetsperiode, Tidslinje(), *aktivitetspliktDokument)
    }
}
