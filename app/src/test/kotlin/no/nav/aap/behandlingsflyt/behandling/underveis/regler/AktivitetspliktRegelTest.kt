package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.AktivitetspliktVurdering.Vilkårsvurdering.AKTIVT_BIDRAG_IKKE_OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_7
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_8
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_9
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_AKTIVT_BIDRAG
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_MØTT_TIL_TILTAK
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Tidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertNull

class AktivitetspliktRegelTest {

    @Test
    fun `11_7 blir vurdert`() {
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2022, 12, 31)),
            brudd(
                bruddType = IKKE_AKTIVT_BIDRAG,
                paragraf = PARAGRAF_11_7,
                periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1)),
                opprettet = LocalDate.of(2020, 1, 2)
            ),
        )
        assertEquals(1, vurderinger.segmenter().size)
        (1..5).forEach { dag ->
            vurderinger.segment(LocalDate.of(2020, 1, dag))!!.verdi.also {
                assertEquals(AKTIVT_BIDRAG_IKKE_OPPFYLT, it.vilkårsvurdering)
            }
        }
    }

    @Test
    fun `11_7 brudd stopper når et annet starter`() {
        val periode1 = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1))
        val periode2 = Periode(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 1))
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2022, 12, 31)),
            brudd(
                bruddType = IKKE_AKTIVT_BIDRAG,
                paragraf = PARAGRAF_11_7,
                periode = periode1,
                opprettet = LocalDate.of(2020, 1, 2)
            ),
            brudd(
                bruddType = IKKE_AKTIVT_BIDRAG,
                paragraf = PARAGRAF_11_7,
                periode = periode2,
                opprettet = LocalDate.of(2020, 1, 2)
            ),
        )
        assertEquals(2, vurderinger.segmenter().size)
        assertNull(vurderinger.segment(periode1.fom.minusDays(1)))

        vurderinger.segment(periode1.fom)!!.verdi.also {
            assertEquals(periode1, it.dokument.brudd.periode)
        }
        vurderinger.segment(periode2.fom.minusDays(1))!!.verdi.also {
            assertEquals(periode1, it.dokument.brudd.periode)
        }
        vurderinger.segment(periode2.fom)!!.verdi.also {
            assertEquals(periode2, it.dokument.brudd.periode)
        }
    }

    @Test
    fun `andre paragrafer blir ikke vurdert`() {
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2022, 12, 31)),
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1)),
                opprettet = LocalDate.of(2020, 1, 2)
            ),
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_9,
                periode = Periode(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 1, 1)),
                opprettet = LocalDate.of(2020, 1, 2)
            ),
        )
        assertEquals(0, vurderinger.segmenter().size)
    }

    private fun vurder(
        rettighetsperiode: Periode,
        vararg aktivitetspliktDokument: AktivitetspliktDokument,
    ): Tidslinje<AktivitetspliktVurdering> {
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            aktivitetspliktDokument = aktivitetspliktDokument.toSet(),
        )
        val vurdering = AktivitetspliktRegel().vurder(input, Tidslinje())
        return vurdering.mapValue { it.aktivitetspliktVurdering!! }
    }
}