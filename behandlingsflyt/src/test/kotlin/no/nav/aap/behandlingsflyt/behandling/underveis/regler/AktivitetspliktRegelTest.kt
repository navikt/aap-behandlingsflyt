package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.AktivitetspliktVurdering.Vilkårsvurdering.AKTIVT_BIDRAG_IKKE_OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_7
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_8
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_9
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_AKTIVT_BIDRAG
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_MØTT_TIL_TILTAK
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingId
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AktivitetspliktRegelTest {

    @Test
    fun `11_7 blir vurdert`() {
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2022, 12, 31)),
            brudd(
                bruddType = IKKE_AKTIVT_BIDRAG,
                paragraf = PARAGRAF_11_7,
                periode = Periode(LocalDate.of(2020, 1, 1), Tid.MAKS),
                opprettet = LocalDate.of(2020, 1, 2)
            ),
        )

        vurderinger.assertTidslinje(
            Segment(Periode(1 januar 2020, 31 desember 2022)) {
                assertEquals(AKTIVT_BIDRAG_IKKE_OPPFYLT, it.vilkårsvurdering)
            }
        )
    }

    @Test
    fun `11_7 brudd stopper når et annet starter`() {
        val dokument1 = InnsendingId.ny()
        val dokument2 = InnsendingId.ny()
        val periode1 = Periode(LocalDate.of(2020, 1, 1), Tid.MAKS)
        val periode2 = Periode(LocalDate.of(2020, 2, 1), Tid.MAKS)
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2022, 12, 31)),
            brudd(
                bruddType = IKKE_AKTIVT_BIDRAG,
                paragraf = PARAGRAF_11_7,
                periode = periode1,
                opprettet = LocalDate.of(2020, 1, 2),
                innsendingId = dokument1,
            ),
            brudd(
                bruddType = IKKE_AKTIVT_BIDRAG,
                paragraf = PARAGRAF_11_7,
                periode = periode2,
                opprettet = LocalDate.of(2020, 1, 3),
                innsendingId = dokument2,
            ),
        )

        vurderinger.assertTidslinje(
            Segment(Periode(1 januar 2020, 31 januar 2020)) {
                assertEquals(AKTIVT_BIDRAG_IKKE_OPPFYLT, it.vilkårsvurdering)
                assertEquals(dokument1, it.dokument.metadata.innsendingId)
            },
            Segment(Periode(1 februar 2020, 31 desember  2022)) {
                assertEquals(AKTIVT_BIDRAG_IKKE_OPPFYLT, it.vilkårsvurdering)
                assertEquals(dokument2, it.dokument.metadata.innsendingId)
            }
        )
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