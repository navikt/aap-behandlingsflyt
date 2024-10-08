package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_INNTIL_EN_DAG
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_STERKE_VELFERDSGRUNNER
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.STANS_ANDRE_DAG
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.STANS_TI_DAGER_BRUKT_OPP
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Grunn.INGEN_GYLDIG_GRUNN
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Grunn.STERKE_VELFERDSGRUNNER
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Paragraf.PARAGRAF_11_8
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Type.IKKE_MØTT_TIL_TILTAK
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetspliktId
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.HendelseId
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.InnsendingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.sakogbehandling.NavIdent
import no.nav.aap.verdityper.sakogbehandling.SakId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Period
import no.nav.aap.tidslinje.Segment

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
                aktivitetsType = IKKE_MØTT_TIL_TILTAK,
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
    fun `ett fravær fra tiltak uten grunn, kan fortsatt stoppes etter tre måneder`() {
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = dato(2020, 1, 1), tom = dato(2022, 12, 31)),
            brudd(
                aktivitetsType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 1), dato(2020, 1, 2)),
                opprettet = dato(2020, 4, 1),
            ),
        )

        /* Kan sanksjonere med 11-9 fordi 2020-04-01 ikke er senere enn tre månder fra 2020-01-01, men
         * er nøyaktigt 3 måneder etter. */
        val vurdering = vurderinger.segment(dato(2020, 1, 2))!!.verdi
        assertEquals(STANS_ANDRE_DAG, vurdering.vilkårsvurdering)
    }

    @Test
    fun `andre dag i meldeperiode kan føre til § 11-8 stans`() {
        val vurdering = vurder(
            rettighetsperiode = Periode(fom = dato(2020, 1, 1), tom = dato(2022, 12, 31)),
            brudd(
                aktivitetsType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 1), dato(2020, 1, 2)),
                opprettet = dato(2020, 4, 1),
            ),
        )
        assertEquals(UNNTAK_INNTIL_EN_DAG, vurdering.segment(dato(2020, 1, 1))!!.verdi.vilkårsvurdering)
        assertEquals(STANS_ANDRE_DAG, vurdering.segment(dato(2020, 1, 2))!!.verdi.vilkårsvurdering)
    }

    @Test
    fun `to fravær i hver sin meldeperiode fører ikke til § 11-8 stans`() {
        val vurdering = vurder(
            rettighetsperiode = Periode(fom = dato(2020, 1, 1), tom = dato(2022, 12, 31)),
            brudd(
                aktivitetsType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 1), dato(2020, 1, 1)),
                opprettet = dato(2020, 4, 1),
            ),
            brudd(
                aktivitetsType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 15), dato(2020, 1, 15)),
                opprettet = dato(2020, 4, 1),
            ),
        )
        assertEquals(UNNTAK_INNTIL_EN_DAG, vurdering.segment(dato(2020, 1, 1))!!.verdi.vilkårsvurdering)
        assertEquals(UNNTAK_INNTIL_EN_DAG, vurdering.segment(dato(2020, 1, 15))!!.verdi.vilkårsvurdering)
    }

    @Test
    fun `12 dager med gydlig fravær i en meldeperiode kan gi stans fra siste fraværsdag`() {
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = dato(2020, 1, 1), tom = dato(2022, 12, 31)),
            brudd(
                aktivitetsType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 1), dato(2020, 1, 12)),
                opprettet = dato(2020, 4, 1),
                grunn = STERKE_VELFERDSGRUNNER,
            ),
        )

        /* Fravær dag 1: telles ikke mot 10-dagers-kvote pga "inntil én dags fravær i meldeperiode"-regelen. */
        vurderinger.segment(dato(2020, 1, 1))!!.verdi.also {
            assertEquals(UNNTAK_INNTIL_EN_DAG, it.vilkårsvurdering)
        }

        /* Fravær dagene 2 – 11: gir ikke stans pga. gyldig grunn, men bruker opp 10 dager av kvoten for kalenderåret. */
        for (dag in 2..11) {
            val muligeUtfall = vurderinger.segment(dato(2020, 1, dag))!!.verdi.vilkårsvurdering
            assertEquals(UNNTAK_STERKE_VELFERDSGRUNNER, muligeUtfall)
        }

        /* Fravær dag 12: gyldig grunn, men gir stans fordi kvoten er brukt opp. */
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
                aktivitetsType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 1), dato(2020, 1, 5)),
                opprettet = dato(2020, 4, 1),
                grunn = STERKE_VELFERDSGRUNNER,
            ),
            /* Tolv brudd i første meldeperiode (2021). Meldeperioden starter den 6. januar. */
            brudd(
                aktivitetsType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(
                    fom = dato(2021, 1, startMeldeperiode2021),
                    tom = dato(2021, 1, startMeldeperiode2021 + 11),
                ),
                opprettet = dato(2020, 4, 1),
                grunn = STERKE_VELFERDSGRUNNER,
            ),
        )

        /* Første dag i 2021, inntil én-dags-regelen */
        vurderinger.segment(dato(2021, 1, startMeldeperiode2021))!!.verdi.also {
            assertEquals(UNNTAK_INNTIL_EN_DAG, it.vilkårsvurdering)
        }

        /* Neste 10 dager i 2021 gir ikke stans pga ti dager fravær per kalenderår. */
        for (dag in (startMeldeperiode2021 + 1)..< startMeldeperiode2021 + 11) {
            val kanStanses = vurderinger.segment(dato(2021, 1, dag))!!.verdi.vilkårsvurdering
            assertEquals(UNNTAK_STERKE_VELFERDSGRUNNER, kanStanses)
        }

        /* Fravær dag 12: gyldig grunn, men gir stans fordi kvoten er brukt opp. */
        vurderinger.segment(dato(2021, 1, startMeldeperiode2021 + 11))!!.verdi.also {
            assertEquals(STANS_TI_DAGER_BRUKT_OPP, it.vilkårsvurdering)
        }
    }

    private fun dato(år: Int, mnd: Int, dag: Int) = LocalDate.of(år, mnd, dag)

    private fun vurder(
        rettighetsperiode: Periode,
        vararg bruddAktivitetsplikt: BruddAktivitetsplikt,
    ): Tidslinje<FraværFastsattAktivitetVurdering> {
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            bruddAktivitetsplikt = bruddAktivitetsplikt.toSet(),
        )
        val vurdering = FraværFastsattAktivitetRegel().vurder(input, Tidslinje())
        return vurdering.mapValue { it.fraværFastsattAktivitetVurdering!! }
    }

    private fun underveisInput(
        rettighetsperiode: Periode,
        bruddAktivitetsplikt: Set<BruddAktivitetsplikt> = setOf(),
    ) = UnderveisInput(
        rettighetsperiode = rettighetsperiode,
        relevanteVilkår = listOf(),
        opptrappingPerioder = listOf(),
        pliktkort = listOf(),
        innsendingsTidspunkt = mapOf(),
        dødsdato = null,
        kvote = Kvote(Period.ofYears(365 * 3)),
        bruddAktivitetsplikt = Tidslinje(bruddAktivitetsplikt.sortedBy { it.periode.fom }.map { Segment(it.periode, it) }),
        etAnnetSted = listOf(),
        barnetillegg = BarnetilleggGrunnlag(1, listOf()),
    )

    private fun brudd(
        aktivitetsType: BruddAktivitetsplikt.Type,
        paragraf: BruddAktivitetsplikt.Paragraf,
        periode: Periode,
        opprettet: LocalDate = periode.tom.plusMonths(4),
        grunn: BruddAktivitetsplikt.Grunn = INGEN_GYLDIG_GRUNN,
    ) = BruddAktivitetsplikt(
        id = BruddAktivitetspliktId(0),
        hendelseId = HendelseId.ny(),
        innsendingId = InnsendingId.ny(),
        navIdent = NavIdent(""),
        sakId = SakId(1),
        type = aktivitetsType,
        paragraf = paragraf,
        begrunnelse = "Informasjon fra tiltaksarrangør",
        periode = periode,
        opprettetTid = opprettet.atStartOfDay(),
        grunn = grunn,
    )
}
