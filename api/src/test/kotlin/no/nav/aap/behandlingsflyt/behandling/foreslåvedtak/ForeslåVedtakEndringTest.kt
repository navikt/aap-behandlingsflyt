package no.nav.aap.behandlingsflyt.behandling.foreslåvedtak

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ForeslåVedtakEndringTest {

    private val start = LocalDate.of(2025, 1, 1)
    private val stansFom = LocalDate.of(2025, 3, 15)
    private val gjenopptattFom = LocalDate.of(2025, 3, 31)
    private val andreStansFom = LocalDate.of(2025, 8, 15)

    private fun oppfylt(fom: LocalDate, tom: LocalDate) =
        Segment(
            Periode(fom, tom),
            ForeslåVedtakData(Utfall.OPPFYLT, RettighetsType.BISTANDSBEHOV, null)
        )

    private fun ikkeOppfylt(fom: LocalDate, tom: LocalDate = Tid.MAKS) =
        Segment(
            Periode(fom, tom),
            ForeslåVedtakData(Utfall.IKKE_OPPFYLT, null, null)
        )

    private fun dato(måned: Int, dag: Int) = LocalDate.of(2025, måned, dag)

    @Test
    fun `ingen forrige behandling gir ingen endring`() {
        val resultat = utledEndringFraForrige(
            segment = Periode(start, dato(12, 31)),
            utfall = Utfall.OPPFYLT,
            oppfyltIndex = 0,
            forrigeSegmenter = null,
        )

        assertThat(resultat.erNy).isFalse()
        assertThat(resultat.tidligerePerioder).isEmpty()
        assertThat(resultat.tidligereVarÅpen).isFalse()
    }

    @Test
    fun `uendret innvilget periode gir ingen endring`() {
        val periode = Periode(start, dato(3, 14))

        val resultat = utledEndringFraForrige(
            segment = periode,
            utfall = Utfall.OPPFYLT,
            oppfyltIndex = 0,
            forrigeSegmenter = listOf(oppfylt(periode.fom, periode.tom)),
        )

        assertThat(resultat.erNy).isFalse()
        assertThat(resultat.tidligerePerioder).isEmpty()
    }

    @Test
    fun `innvilget periode med endret tom er endret og viser kun forrige behandlings tom`() {
        val resultat = utledEndringFraForrige(
            segment = Periode(start, dato(3, 14)),
            utfall = Utfall.OPPFYLT,
            oppfyltIndex = 0,
            forrigeSegmenter = listOf(oppfylt(start, dato(12, 31))),
        )

        assertThat(resultat.erNy).isFalse()
        assertThat(resultat.tidligerePerioder).containsExactly(Periode(start, dato(12, 31)))
    }

    @Test
    fun `tom endret i tidligere behandling vises ikke igjen når den er uendret fra forrige`() {
        val forrige = listOf(
            oppfylt(start, dato(3, 14)),
            ikkeOppfylt(stansFom, dato(3, 30)),
            oppfylt(gjenopptattFom, dato(12, 31)),
        )

        val rad1 = utledEndringFraForrige(Periode(start, dato(3, 14)), Utfall.OPPFYLT, 0, forrige)

        assertThat(rad1.erNy).isFalse()
        assertThat(rad1.tidligerePerioder).isEmpty()
    }

    @Test
    fun `ny innvilget periode etter stans er ny uten tidligere perioder`() {
        val forrige = listOf(
            oppfylt(start, dato(3, 14)),
            ikkeOppfylt(stansFom),
        )

        val resultat = utledEndringFraForrige(
            segment = Periode(gjenopptattFom, dato(12, 31)),
            utfall = Utfall.OPPFYLT,
            oppfyltIndex = 1,
            forrigeSegmenter = forrige,
        )

        assertThat(resultat.erNy).isTrue()
        assertThat(resultat.tidligerePerioder).isEmpty()
    }

    @Test
    fun `endret tom på senere innvilget periode er endret og viser forrige tom`() {
        val forrige = listOf(
            oppfylt(start, dato(3, 14)),
            ikkeOppfylt(stansFom, dato(3, 30)),
            oppfylt(gjenopptattFom, dato(12, 31)),
        )

        val resultat = utledEndringFraForrige(
            segment = Periode(gjenopptattFom, dato(8, 14)),
            utfall = Utfall.OPPFYLT,
            oppfyltIndex = 1,
            forrigeSegmenter = forrige,
        )

        assertThat(resultat.erNy).isFalse()
        assertThat(resultat.tidligerePerioder).containsExactly(Periode(gjenopptattFom, dato(12, 31)))
    }

    @Test
    fun `uendret stans gir ingen endring`() {
        val forrige = listOf(
            oppfylt(start, dato(3, 14)),
            ikkeOppfylt(stansFom),
        )

        val resultat = utledEndringFraForrige(
            segment = Periode(stansFom, Tid.MAKS),
            utfall = Utfall.IKKE_OPPFYLT,
            oppfyltIndex = -1,
            forrigeSegmenter = forrige,
        )

        assertThat(resultat.erNy).isFalse()
        assertThat(resultat.tidligerePerioder).isEmpty()
        assertThat(resultat.tidligereVarÅpen).isFalse()
    }

    @Test
    fun `stans som får sluttdato var åpen i forrige behandling`() {
        val forrige = listOf(
            oppfylt(start, dato(3, 14)),
            oppfylt(gjenopptattFom, dato(8, 14)),
            ikkeOppfylt(andreStansFom),
        )

        val resultat = utledEndringFraForrige(
            segment = Periode(andreStansFom, dato(9, 12)),
            utfall = Utfall.IKKE_OPPFYLT,
            oppfyltIndex = -1,
            forrigeSegmenter = forrige,
        )

        assertThat(resultat.erNy).isFalse()
        assertThat(resultat.tidligerePerioder).containsExactly(Periode(andreStansFom, Tid.MAKS))
        assertThat(resultat.tidligereVarÅpen).isTrue()
    }

    @Test
    fun `stans med endret sluttdato er ikke åpen i forrige behandling`() {
        val forrige = listOf(
            oppfylt(start, dato(3, 14)),
            ikkeOppfylt(stansFom, dato(3, 30)),
            oppfylt(gjenopptattFom, dato(12, 31)),
        )

        val resultat = utledEndringFraForrige(
            segment = Periode(stansFom, dato(4, 10)),
            utfall = Utfall.IKKE_OPPFYLT,
            oppfyltIndex = -1,
            forrigeSegmenter = forrige,
        )

        assertThat(resultat.tidligerePerioder).containsExactly(Periode(stansFom, dato(3, 30)))
        assertThat(resultat.tidligereVarÅpen).isFalse()
    }

    @Test
    fun `ikke oppfylt periode som ikke fantes i forrige behandling gir ingen tidligere perioder`() {
        val forrige = listOf(oppfylt(start, dato(12, 31)))

        val resultat = utledEndringFraForrige(
            segment = Periode(stansFom, Tid.MAKS),
            utfall = Utfall.IKKE_OPPFYLT,
            oppfyltIndex = -1,
            forrigeSegmenter = forrige,
        )

        assertThat(resultat.tidligerePerioder).isEmpty()
        assertThat(resultat.tidligereVarÅpen).isFalse()
    }

    @Test
    fun `ikke oppfylt periode uten forrige er ny`() {
        val resultat = utledEndringFraForrige(
            segment = Periode(stansFom, Tid.MAKS),
            utfall = Utfall.IKKE_OPPFYLT,
            oppfyltIndex = -1,
            forrigeSegmenter = listOf(oppfylt(start, dato(12, 31))),
        )

        assertThat(resultat.erNy).isTrue()
    }

    @Test
    fun `ikke oppfylt periode med endret fom er endret og viser forrige periode`() {
        val resultat = utledEndringFraForrige(
            segment = Periode(dato(3, 20), dato(3, 30)),
            utfall = Utfall.IKKE_OPPFYLT,
            oppfyltIndex = -1,
            forrigeSegmenter = listOf(oppfylt(start, dato(3, 14)), ikkeOppfylt(stansFom, dato(3, 30))),
        )

        assertThat(resultat.erNy).isFalse()
        assertThat(resultat.tidligerePerioder).containsExactly(Periode(stansFom, dato(3, 30)))
    }

    @Test
    fun `tom forrige liste gir ingen endring`() {
        val resultat = utledEndringFraForrige(
            segment = Periode(start, dato(12, 31)),
            utfall = Utfall.OPPFYLT,
            oppfyltIndex = 0,
            forrigeSegmenter = emptyList(),
        )

        assertThat(resultat.erNy).isFalse()
        assertThat(resultat.tidligerePerioder).isEmpty()
    }
}
