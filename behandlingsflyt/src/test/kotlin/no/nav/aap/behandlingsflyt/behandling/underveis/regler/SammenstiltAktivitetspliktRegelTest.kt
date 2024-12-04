package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_7
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_8
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_9
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_AKTIVT_BIDRAG
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_MØTT_TIL_MØTE
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_MØTT_TIL_TILTAK
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_SENDT_INN_DOKUMENTASJON
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Grunn.BIDRAR_AKTIVT
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Grunn.INGEN_GYLDIG_GRUNN
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Grunn.RIMELIG_GRUNN
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Grunn.STERKE_VELFERDSGRUNNER
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SammenstiltAktivitetspliktRegelTest {

    @Test
    fun `11-7 har prioritet over 11-8`() {
        val fraværFastsattAktivitetPeriode = Periode(
            fom = LocalDate.of(2020, 1, 4),
            tom = LocalDate.of(2020, 1, 8),
        )
        val aktivitetspliktBruddPeriode = Periode(LocalDate.of(2020, 1, 7), Tid.MAKS)

        val rettighetsperiode = Periode(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2022, 12, 31))
        val aktivitetsbruddVurderinger = aktivitetsbruddVurderinger(
            rettighetsperiode = rettighetsperiode,
            startTidslinje = Tidslinje(),
            brudd(
                bruddType = IKKE_AKTIVT_BIDRAG,
                paragraf = PARAGRAF_11_7,
                periode = aktivitetspliktBruddPeriode,
                opprettet = LocalDate.of(2020, 1, 1),
                grunn = INGEN_GYLDIG_GRUNN,
            ),
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = fraværFastsattAktivitetPeriode,
                opprettet = LocalDate.of(2020, 1, 1),
                grunn = STERKE_VELFERDSGRUNNER,
            )
        )

        aktivitetsbruddVurderinger.segment(fraværFastsattAktivitetPeriode.fom).also {
            assertEquals(
                Periode(fraværFastsattAktivitetPeriode.fom, aktivitetspliktBruddPeriode.fom.minusDays(1)),
                it?.periode
            )
            assertNotNull(it?.verdi?.fraværFastsattAktivitetVurdering)
            assertNull(it?.verdi?.aktivitetspliktVurdering)
        }

        aktivitetsbruddVurderinger.kombiner<_, Nothing>(Tidslinje(
            Periode(
                aktivitetspliktBruddPeriode.fom,
                rettighetsperiode.tom
            ), Unit
        ),
            JoinStyle.RIGHT_JOIN { _, vurdering, _ ->
                assertNotNull(vurdering?.verdi?.aktivitetspliktVurdering)
                assertNull(vurdering?.verdi?.fraværFastsattAktivitetVurdering)
                null
            }
        )
    }

    @Test
    fun `11-7 og 11-8 har prioritet over 11-9`() {
        val fraværFastsattAktivitetPeriode = Periode(
            fom = LocalDate.of(2020, 1, 4),
            tom = LocalDate.of(2020, 1, 6),
        )
        val aktivitetspliktBruddPeriode = Periode(LocalDate.of(2020, 1, 7), Tid.MAKS)
        val reduksjonAktivitetspliktPeriode = Periode(
            fom = LocalDate.of(2020, 1, 2),
            tom = LocalDate.of(2020, 1, 8),
        )

        val aktivitetsbruddVurderinger = aktivitetsbruddVurderinger(
            rettighetsperiode = Periode(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2022, 12, 31)),
            startTidslinje = Tidslinje(),
            brudd(
                bruddType = IKKE_AKTIVT_BIDRAG,
                paragraf = PARAGRAF_11_7,
                periode = aktivitetspliktBruddPeriode,
                opprettet = LocalDate.of(2020, 1, 1),
                grunn = INGEN_GYLDIG_GRUNN,
            ),
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = fraværFastsattAktivitetPeriode,
                opprettet = LocalDate.of(2020, 1, 1),
                grunn = STERKE_VELFERDSGRUNNER,
            ),
            brudd(
                bruddType = IKKE_MØTT_TIL_MØTE,
                paragraf = PARAGRAF_11_9,
                periode = reduksjonAktivitetspliktPeriode,
                opprettet = LocalDate.of(2020, 1, 1),
                grunn = STERKE_VELFERDSGRUNNER,
            )
        )

        aktivitetsbruddVurderinger.segment(reduksjonAktivitetspliktPeriode.fom).also {
            assertEquals(
                Periode(reduksjonAktivitetspliktPeriode.fom, fraværFastsattAktivitetPeriode.fom.minusDays(1)),
                it?.periode
            )
            assertNotNull(it?.verdi?.reduksjonAktivitetspliktVurdering)
            assertNull(it?.verdi?.fraværFastsattAktivitetVurdering)
            assertNull(it?.verdi?.aktivitetspliktVurdering)
        }

        aktivitetsbruddVurderinger.segment(fraværFastsattAktivitetPeriode.fom).also {
            assertNull(it?.verdi?.reduksjonAktivitetspliktVurdering)
        }

        aktivitetsbruddVurderinger.segment(aktivitetspliktBruddPeriode.fom).also {
            assertNull(it?.verdi?.reduksjonAktivitetspliktVurdering)
        }
    }

    @Test
    fun `11-7 med AKTIVT_BIDRAG overskriver ikke 11-8 og 11-9`() {
        val aktivitetsbruddVurderinger = aktivitetsbruddVurderinger(
            rettighetsperiode = Periode(1 januar 2020, 31 desember 2022),
            startTidslinje = Tidslinje(),
            brudd(
                bruddType = IKKE_AKTIVT_BIDRAG,
                paragraf = PARAGRAF_11_7,
                periode = Periode(1 januar 2020, Tid.MAKS),
                opprettet = LocalDate.of(2020, 1, 1),
                grunn = BIDRAR_AKTIVT,
            ),
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(1 januar 2020, 3 januar 2020),
                opprettet = LocalDate.of(2020, 1, 1),
                grunn = STERKE_VELFERDSGRUNNER,
            ),
            brudd(
                bruddType = IKKE_SENDT_INN_DOKUMENTASJON,
                paragraf = PARAGRAF_11_9,
                periode = Periode(4 januar 2020, 6 januar 2020),
                opprettet = LocalDate.of(2020, 1, 1),
                grunn = RIMELIG_GRUNN,
            )
        )

        aktivitetsbruddVurderinger.assertTidslinje(
            Segment(Periode(1 januar 2020, 3 januar 2020)) {
                assertNotNull(it.fraværFastsattAktivitetVurdering)
                assertNull(it.aktivitetspliktVurdering)
                assertNull(it.reduksjonAktivitetspliktVurdering)
            },
            Segment(Periode(4 januar 2020, 6 januar 2020)) {
                assertNotNull(it.reduksjonAktivitetspliktVurdering)
                assertNull(it.aktivitetspliktVurdering)
                assertNull(it.fraværFastsattAktivitetVurdering)
            }
        )
    }
}

private fun aktivitetsbruddVurderinger(
    rettighetsperiode: Periode,
    startTidslinje: Tidslinje<Vurdering>,
    vararg aktivitetspliktDokument: AktivitetspliktDokument,
): Tidslinje<Vurdering> {
    val input = underveisInput(
        rettighetsperiode = rettighetsperiode,
        aktivitetspliktDokument = aktivitetspliktDokument.toSet(),
    )
    return SammenstiltAktivitetspliktRegel().vurder(input, UtledMeldeperiodeRegel().vurder(input, startTidslinje))
        .filter { it.verdi.fraværFastsattAktivitetVurdering != null || it.verdi.reduksjonAktivitetspliktVurdering != null || it.verdi.aktivitetspliktVurdering != null }
        .mapValue { it.copy(meldepliktVurdering = null) }.komprimer()
}