package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ReduksjonAktivitetspliktVurdering.Vilkårsvurdering.FORELDET
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ReduksjonAktivitetspliktVurdering.Vilkårsvurdering.VILKÅR_FOR_REDUKSJON_OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_7
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_8
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_9
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_AKTIVT_BIDRAG
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_MØTT_TIL_TILTAK
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.tidslinje.Tidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ReduksjonFraværFastsattAktivitetRegelTest {
    @Test
    fun `ingen brudd, ingen vurderinger`() {
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2022, 12, 31))
        )
        assertEquals(0, vurderinger.segmenter().size)
    }

    @Test
    fun `vurderinger fra paragraf 11_8 og 11_7 blir ikke med`() {
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2022, 12, 31)),
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1)),
                opprettet = LocalDate.of(2020, 1, 2)
            ),
            brudd(
                bruddType = IKKE_AKTIVT_BIDRAG,
                paragraf = PARAGRAF_11_7,
                periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 1)),
                opprettet = LocalDate.of(2020, 1, 2)
            ),
        )
        assertEquals(0, vurderinger.segmenter().size)
    }

    @Test
    fun `foreldede brudd uten gyldig grunn blir ikke sanksjonert`() {
        val bruddDato = LocalDate.of(2020, 1, 1)
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = bruddDato, tom = LocalDate.of(2022, 12, 31)),
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_9,
                periode = Periode(bruddDato, bruddDato),
                opprettet = LocalDate.of(2020, 4, 1),
            ),
        )
        assertEquals(1, vurderinger.segmenter().size)

        /* Kan ikke sanksjonere med 11-9 fordi 2020-04-01 er senere enn tre månder fra 2020-01-01, men
         * er en dag mer enn 3 måneder etter. */
        val vurdering = vurderinger.segment(bruddDato)!!.verdi
        assertEquals(FORELDET, vurdering.vilkårsvurdering)
    }

    @Test
    fun `nye brudd uten gyldig grunn blir sanksjonert`() {
        val bruddDato = LocalDate.of(2020, 1, 1)
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = bruddDato, tom = LocalDate.of(2022, 12, 31)),
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_9,
                periode = Periode(bruddDato, bruddDato),
                opprettet = LocalDate.of(2020, 3, 31),
            ),
        )
        assertEquals(1, vurderinger.segmenter().size)

        assertNull(vurderinger.segment(LocalDate.of(2019, 12, 31)))
        /* Kan sanksjonere med 11-9 fordi 2020-03-31 ikke er senere enn tre månder fra 2020-01-01, men
         * er nøyaktigt 3 måneder etter. */
        val vurdering = vurderinger.segment(bruddDato)!!.verdi
        assertEquals(VILKÅR_FOR_REDUKSJON_OPPFYLT, vurdering.vilkårsvurdering)

        assertNull(vurderinger.segment(LocalDate.of(2020, 1, 2)))
    }


    @Test
    fun `periode med både foreldede og ikke-foreldede brudd blir håndtert`() {
        val førsteBruddag = LocalDate.of(2020, 1, 1)
        val andreBruddag = LocalDate.of(2020, 1, 2)
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = førsteBruddag, tom = LocalDate.of(2022, 12, 31)),
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_9,
                periode = Periode(førsteBruddag, andreBruddag),
                opprettet = LocalDate.of(2020, 4, 1),
            ),
        )
        assertEquals(2, vurderinger.segmenter().size)

        /* Kan ikke sanksjonere med 11-9 fordi 2020-04-01 er senere enn tre månder fra 2020-01-01, men
         * er en dag mer enn 3 måneder etter. */
        vurderinger.segment(førsteBruddag)!!.verdi.also {
            assertEquals(FORELDET, it.vilkårsvurdering)
        }
        vurderinger.segment(andreBruddag)!!.verdi.also {
            assertEquals(VILKÅR_FOR_REDUKSJON_OPPFYLT, it.vilkårsvurdering)
        }
    }

    @Test
    fun `like vurderinger bli komprimert`() {
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2022, 12, 31)),
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_9,
                periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 2)),
                opprettet = LocalDate.of(2020, 3, 31),
            ),
        )
        assertEquals(1, vurderinger.segmenter().size)
    }

    private fun vurder(
        rettighetsperiode: Periode,
        vararg aktivitetspliktDokument: AktivitetspliktDokument,
    ): Tidslinje<ReduksjonAktivitetspliktVurdering> {
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            aktivitetspliktDokument = aktivitetspliktDokument.toSet(),
        )
        val vurdering = ReduksjonAktivitetspliktRegel().vurder(input, Tidslinje())
        return vurdering.mapValue { it.reduksjonAktivitetspliktVurdering!! }
    }
}