package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.Kvote
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Grunn.INGEN_GYLDIG_GRUNN
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Grunn.STERKE_VELFERDSGRUNNER
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Paragraf.PARAGRAF_11_8
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Paragraf.PARAGRAF_11_9
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

class AktivitetspliktRegelTest {
    @Test
    fun `ingen brudd, ingen vurderinger`() {
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = dato(2020, 1, 1), tom = dato(2022, 12, 31))
        )
        assertEquals(0, vurderinger.segmenter().size)
    }

    @Test
    fun `ett fravær fra tiltak, kun 11-9 kan brukes`() {
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
        /* Kan ikke sanksjonere med 11-8 på grunn av regelen om én dags fravær i meldeperioden. */
        assertEquals(listOf(PARAGRAF_11_9), vurdering.muligeSanksjoner)
    }

    @Test
    fun `ett fravær fra tiltak uten grunn, kan ikke sanksjoneres etter tre måneder`() {
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = dato(2020, 1, 1), tom = dato(2022, 12, 31)),
            brudd(
                aktivitetsType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 1), dato(2020, 1, 1)),
                opprettet = dato(2020, 4, 1),
            ),
        )
        assertEquals(1, vurderinger.segmenter().size)

        /* Kan sanksjonere med 11-9 fordi 2020-04-01 ikke er senere enn tre månder fra 2020-01-01, men
         * er nøyaktigt 3 måneder etter. */
        /* Kan ikke sanksjonere med 11-8 på grunn av regelen om én dags fravær i meldeperioden. */
        val vurdering = vurderinger.segment(dato(2020, 1, 1))!!.verdi
        assertEquals(listOf(PARAGRAF_11_9), vurdering.muligeSanksjoner)
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
        assertEquals(listOf(PARAGRAF_11_9), vurdering.segment(dato(2020, 1, 1))!!.verdi.muligeSanksjoner)
        assertEquals(listOf(PARAGRAF_11_8, PARAGRAF_11_9), vurdering.segment(dato(2020, 1, 2))!!.verdi.muligeSanksjoner)
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
        assertEquals(listOf(PARAGRAF_11_9), vurdering.segment(dato(2020, 1, 1))!!.verdi.muligeSanksjoner)
        assertEquals(listOf(PARAGRAF_11_9), vurdering.segment(dato(2020, 1, 15))!!.verdi.muligeSanksjoner)
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

        /* Ingen av dagene kan gi reduksjon etter 11-9 pga. gyldig grunn. */
        /* Fravær dag 1: telles ikke mot 10-dagers-kvote pga "inntil én dags fravær i meldeperiode"-regelen. */
        /* Fravær dagene 2 – 11: gir ikke stans pga. gyldig grunn, men bruker opp 10 dager av kvoten for kalenderåret. */
        for (dag in 1..11) {
            val muligeSanksjoner = vurderinger.segment(dato(2020, 1, dag))!!.verdi.muligeSanksjoner
            assertEquals(listOf<BruddAktivitetsplikt.Paragraf>(), muligeSanksjoner)
        }

        /* Fravær dag 12: gyldig grunn, men gir stans fordi kvoten er brukt opp. */
        val muligeSanksjoner = vurderinger.segment(dato(2020, 1, 12))!!.verdi.muligeSanksjoner
        assertEquals(listOf(PARAGRAF_11_8), muligeSanksjoner)
    }

    @Test
    fun `10-dagers-kvote starter på 0 i nytt kalenderår`() {
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
                periode = Periode(fom = dato(2021, 1, 6), tom = dato(2021, 1, 6 + 11)),
                opprettet = dato(2020, 4, 1),
                grunn = STERKE_VELFERDSGRUNNER,
            ),
        )

        /* Ingen av dagene kan gi reduksjon etter 11-9 pga. gyldig grunn. */

        /* For 2021:
         * Fravær første dag (den 6. januar) gir ikke stans:
         * - telles ikke mot 10-dagers-kvote pga "inntil én dags fravær i meldeperiode"-regelen.
         * Fravær dagene 2. til 11. gir ikke stans:
         * - selv om 4 dager ble brukt av kvoten i 2020, så ble den resatt, så det er 10 dagers kvote i 2021.
         * - gir ikke stans pga. gyldig grunn, men bruker opp 10 dager av kvoten for kalenderåret 2021.
         *  */
        for (dag in 6..<6 + 11) {
            val muligeSanksjoner = vurderinger.segment(dato(2021, 1, dag))!!.verdi.muligeSanksjoner
            assertEquals(listOf<BruddAktivitetsplikt.Paragraf>(), muligeSanksjoner)
        }

        /* Fravær dag 12: gyldig grunn, men gir stans fordi kvoten er brukt opp. */
        val muligeSanksjoner = vurderinger.segment(dato(2021, 1, 6 + 11))!!.verdi.muligeSanksjoner
        assertEquals(listOf(PARAGRAF_11_8), muligeSanksjoner)
    }

    private fun dato(år: Int, mnd: Int, dag: Int) = LocalDate.of(år, mnd, dag)

    private fun vurder(
        rettighetsperiode: Periode,
        vararg bruddAktivitetsplikt: BruddAktivitetsplikt,
    ): Tidslinje<AktivitetspliktVurdering> {
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            bruddAktivitetsplikt = bruddAktivitetsplikt.toSet(),
        )
        val vurdering = AktivitetspliktRegel().vurder(input, Tidslinje())
        return vurdering.mapValue { it.aktivitetspliktVurdering!! }
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
        bruddAktivitetsplikt = bruddAktivitetsplikt,
        etAnnetSted = listOf(),
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