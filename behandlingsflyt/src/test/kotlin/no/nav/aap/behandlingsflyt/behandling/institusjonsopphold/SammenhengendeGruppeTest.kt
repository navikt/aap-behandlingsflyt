package no.nav.aap.behandlingsflyt.behandling.institusjonsopphold

import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SammenhengendeGruppeTest {

    private data class TestPeriode(val fom: LocalDate, val tom: LocalDate)

    private val erSammenhengendeMedNullDagersGap: (Periode, LocalDate) -> Boolean =
        { periode, nesteFom -> !nesteFom.isAfter(periode.tom.plusDays(1)) }

    @Test
    fun `to elementer med null dagers gap slås sammen til én gruppe`() {
        val a = TestPeriode(1 januar 2026, 31 januar 2026)
        val b = TestPeriode(1 februar 2026, 28 februar 2026)

        val grupper = grupperSammenhengende(
            listOf(a, b), { it.fom }, { it.tom }, erSammenhengendeMedNullDagersGap
        )

        assertThat(grupper).hasSize(1)
        assertThat(grupper.first().elementer).containsExactly(a, b)
        assertThat(grupper.first().periode).isEqualTo(Periode(1 januar 2026, 28 februar 2026))
    }

    @Test
    fun `to elementer med reelt gap forblir separate grupper`() {
        val a = TestPeriode(1 januar 2026, 31 januar 2026)
        val b = TestPeriode(3 februar 2026, 28 februar 2026)

        val grupper = grupperSammenhengende(
            listOf(a, b), { it.fom }, { it.tom }, erSammenhengendeMedNullDagersGap
        )

        assertThat(grupper).hasSize(2)
        assertThat(grupper[0].elementer).containsExactly(a)
        assertThat(grupper[1].elementer).containsExactly(b)
    }

    @Test
    fun `tre elementer usortert input grupperes korrekt basert på fom`() {
        val a = TestPeriode(1 januar 2026, 31 januar 2026)
        val b = TestPeriode(1 februar 2026, 28 februar 2026)
        val c = TestPeriode(1 mars 2026, 31 mars 2026)

        val grupper = grupperSammenhengende(
            listOf(a, b, c), { it.fom }, { it.tom }, erSammenhengendeMedNullDagersGap
        )

        assertThat(grupper).hasSize(1)
        assertThat(grupper.first().elementer).containsExactly(a, b, c)
        assertThat(grupper.first().periode).isEqualTo(Periode(1 januar 2026, 31 mars 2026))
    }

    @Test
    fun `tom liste gir tom grupperingsliste`() {
        val grupper = grupperSammenhengende(
            emptyList<TestPeriode>(), { it.fom }, { it.tom }, erSammenhengendeMedNullDagersGap
        )
        assertThat(grupper).isEmpty()
    }

    @Test
    fun `enkelt element gir én gruppe med seg selv`() {
        val a = TestPeriode(1 januar 2026, 31 januar 2026)
        val grupper = grupperSammenhengende(
            listOf(a), { it.fom }, { it.tom }, erSammenhengendeMedNullDagersGap
        )
        assertThat(grupper).hasSize(1)
        assertThat(grupper.first().elementer).containsExactly(a)
        assertThat(grupper.first().periode).isEqualTo(Periode(1 januar 2026, 31 januar 2026))
    }

    @Test
    fun `finnRelevanteInnenforPeriode returnerer kun elementer fra grupper som overlapper perioden`() {
        val a = TestPeriode(1 januar 2026, 31 januar 2026)
        val b = TestPeriode(1 februar 2026, 28 februar 2026) // sammenhengende med a
        val c = TestPeriode(1 april 2026, 30 april 2026) // separat gruppe, ikke relevant

        val relevante = finnRelevanteInnenforPeriode(
            listOf(a, b, c),
            periode = Periode(15 februar 2026, 20 februar 2026), // treffer kun den sammenhengende kjeden a+b
            fom = { it.fom },
            tom = { it.tom },
            erSammenhengende = erSammenhengendeMedNullDagersGap
        )

        assertThat(relevante).containsExactlyInAnyOrder(a, b)
    }

    @Test
    fun `finnRelevanteInnenforPeriode returnerer tom liste når ingen grupper overlapper`() {
        val a = TestPeriode(1 januar 2026, 31 januar 2026)

        val relevante = finnRelevanteInnenforPeriode(
            listOf(a),
            periode = Periode(1 mars 2026, 31 mars 2026),
            fom = { it.fom },
            tom = { it.tom },
            erSammenhengende = erSammenhengendeMedNullDagersGap
        )

        assertThat(relevante).isEmpty()
    }

    @Test
    fun `ulik erSammenhengende-regel gir ulikt grupperingsresultat for samme input`() {
        val a = TestPeriode(1 januar 2026, 31 januar 2026)
        val b = TestPeriode(3 februar 2026, 28 februar 2026) // to dagers gap

        // Regel som tillater opptil 2 dagers gap
        val tolerantRegel: (Periode, LocalDate) -> Boolean =
            { periode, nesteFom -> !nesteFom.isAfter(periode.tom.plusDays(3)) }

        val grupperStrengt = grupperSammenhengende(listOf(a, b), { it.fom }, { it.tom }, erSammenhengendeMedNullDagersGap)
        val grupperTolerant = grupperSammenhengende(listOf(a, b), { it.fom }, { it.tom }, tolerantRegel)

        assertThat(grupperStrengt).hasSize(2)
        assertThat(grupperTolerant).hasSize(1)
    }
}