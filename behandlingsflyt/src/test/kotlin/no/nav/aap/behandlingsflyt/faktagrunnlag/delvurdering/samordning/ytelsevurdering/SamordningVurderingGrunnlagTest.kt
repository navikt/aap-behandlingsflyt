package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SamordningVurderingGrunnlagTest {

    private fun grunnlag(vararg vurderinger: SamordningVurdering) = SamordningVurderingGrunnlag(
        begrunnelse = "b",
        vurderinger = vurderinger.toSet(),
        vurdertAv = "test",
        vurdertTidspunkt = LocalDateTime.now()
    )

    @Test
    fun `tilTidslinje - tom vurderingsliste gir tom tidslinje`() {
        assertThat(grunnlag().tilTidslinje().segmenter()).isEmpty()
    }

    @Test
    fun `tilTidslinje - én vurdering gir ett segment`() {
        val vurdering = SamordningVurdering(
            ytelseType = Ytelse.SYKEPENGER,
            vurderingPerioder = setOf(
                SamordningVurderingPeriode(Periode(1 januar 2026, 31 januar 2026), Prosent.`100_PROSENT`, manuell = true)
            )
        )
        val segmenter = grunnlag(vurdering).tilTidslinje().segmenter()
        assertThat(segmenter).hasSize(1)
        assertThat(segmenter.first().verdi).hasSize(1)
        assertThat(segmenter.first().verdi.first().first).isEqualTo(Ytelse.SYKEPENGER)
    }

    @Test
    fun `tilTidslinje - to ytelser i samme periode slås sammen til ett segment`() {
        val vurdering1 = SamordningVurdering(
            ytelseType = Ytelse.SYKEPENGER,
            vurderingPerioder = setOf(
                SamordningVurderingPeriode(Periode(1 januar 2026, 31 januar 2026), Prosent.`50_PROSENT`, manuell = true)
            )
        )
        val vurdering2 = SamordningVurdering(
            ytelseType = Ytelse.FORELDREPENGER,
            vurderingPerioder = setOf(
                SamordningVurderingPeriode(Periode(1 januar 2026, 31 januar 2026), Prosent.`50_PROSENT`, manuell = true)
            )
        )
        val segmenter = grunnlag(vurdering1, vurdering2).tilTidslinje().segmenter()
        assertThat(segmenter).hasSize(1)
        assertThat(segmenter.first().verdi).hasSize(2)
    }

    @Test
    fun `tilTidslinje - to perioder for samme ytelse gir to segmenter`() {
        val vurdering = SamordningVurdering(
            ytelseType = Ytelse.SYKEPENGER,
            vurderingPerioder = setOf(
                SamordningVurderingPeriode(Periode(1 januar 2026, 15 januar 2026), Prosent.`100_PROSENT`, manuell = true),
                SamordningVurderingPeriode(Periode(16 januar 2026, 31 januar 2026), Prosent.`50_PROSENT`, manuell = true),
            )
        )
        assertThat(grunnlag(vurdering).tilTidslinje().segmenter()).hasSize(2)
    }
}