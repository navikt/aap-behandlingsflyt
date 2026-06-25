package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SamordningYtelseVurderingGrunnlagTest {

    private fun ytelse(
        type: Ytelse = Ytelse.SYKEPENGER,
        periode: Periode = Periode(1 januar 2026, 31 januar 2026),
        gradering: Prosent = Prosent.`100_PROSENT`,
    ) = SamordningYtelseGrunnlag(
        grunnlagId = 1L,
        ytelser = setOf(
            SamordningYtelse(
                ytelseType = type,
                kilde = "kilde",
                ytelsePerioder = setOf(SamordningYtelsePeriode(periode, gradering))
            )
        )
    )

    private fun vurdering(
        type: Ytelse = Ytelse.SYKEPENGER,
        periode: Periode = Periode(1 januar 2026, 31 januar 2026),
        gradering: Prosent = Prosent.`100_PROSENT`,
    ) = SamordningVurderingGrunnlag(
        begrunnelse = "begrunnelse",
        vurderinger = setOf(
            SamordningVurdering(
                ytelseType = type,
                vurderingPerioder = setOf(SamordningVurderingPeriode(periode, gradering, manuell = true))
            )
        ),
        vurdertAv = "test",
        vurdertTidspunkt = LocalDateTime.now()
    )

    @Test
    fun `tilTidslinje - manuell vurdering 100 prosent gir IKKE_OPPFYLT-gradering`() {
        val grunnlag = SamordningYtelseVurderingGrunnlag(
            ytelseGrunnlag = null,
            vurderingGrunnlag = vurdering(gradering = Prosent.`100_PROSENT`),
        )
        val segmenter = grunnlag.tilTidslinje().segmenter()
        assertThat(segmenter).hasSize(1)
        assertThat(segmenter.first().verdi.gradering).isEqualTo(Prosent.`100_PROSENT`)
        assertThat(segmenter.first().verdi.ytelsesGraderinger).hasSize(1)
        assertThat(segmenter.first().verdi.ytelsesGraderinger.first().ytelse).isEqualTo(Ytelse.SYKEPENGER)
    }

    @Test
    fun `tilTidslinje - ingen vurdering og ingen ytelse gir tom tidslinje`() {
        val grunnlag = SamordningYtelseVurderingGrunnlag(null, null)
        assertThat(grunnlag.tilTidslinje().segmenter()).isEmpty()
    }

    @Test
    fun `tilTidslinje - to ytelser i ulike perioder gir to segmenter`() {
        val grunnlag = SamordningYtelseVurderingGrunnlag(
            ytelseGrunnlag = null,
            vurderingGrunnlag = SamordningVurderingGrunnlag(
                begrunnelse = "begrunnelse",
                vurderinger = setOf(
                    SamordningVurdering(
                        ytelseType = Ytelse.SYKEPENGER,
                        vurderingPerioder = setOf(
                            SamordningVurderingPeriode(Periode(1 januar 2026, 10 januar 2026), Prosent.`100_PROSENT`, manuell = true),
                            SamordningVurderingPeriode(Periode(11 januar 2026, 20 januar 2026), Prosent.`50_PROSENT`, manuell = true),
                        )
                    )
                ),
                vurdertAv = "test",
                vurdertTidspunkt = LocalDateTime.now()
            ),
        )
        assertThat(grunnlag.tilTidslinje().segmenter()).hasSize(2)
    }

    @Test
    fun `tidslinjeMedSamordningYtelser - MANUELL ytelse inkluderes`() {
        val grunnlag = SamordningYtelseVurderingGrunnlag(
            ytelseGrunnlag = ytelse(type = Ytelse.SYKEPENGER),
            vurderingGrunnlag = null,
        )
        assertThat(grunnlag.tidslinjeMedSamordningYtelser().segmenter()).hasSize(1)
        assertThat(grunnlag.tidslinjeMedSamordningYtelser().segmenter().first().verdi)
            .contains(Ytelse.SYKEPENGER)
    }

    @Test
    fun `perioderSomIkkeHarBlittVurdert - periode uten vurdering returneres`() {
        val periode = Periode(1 januar 2026, 31 januar 2026)
        val grunnlag = SamordningYtelseVurderingGrunnlag(
            ytelseGrunnlag = ytelse(periode = periode),
            vurderingGrunnlag = null,
        )
        val ikkeVurdert = grunnlag.perioderSomIkkeHarBlittVurdert()
        assertThat(ikkeVurdert.segmenter()).hasSize(1)
        assertThat(ikkeVurdert.segmenter().first().periode).isEqualTo(periode)
    }

    @Test
    fun `perioderSomIkkeHarBlittVurdert - vurdert periode filtreres bort`() {
        val periode = Periode(1 januar 2026, 31 januar 2026)
        val grunnlag = SamordningYtelseVurderingGrunnlag(
            ytelseGrunnlag = ytelse(periode = periode),
            vurderingGrunnlag = vurdering(periode = periode),
        )
        assertThat(grunnlag.perioderSomIkkeHarBlittVurdert().segmenter()).isEmpty()
    }

    @Test
    fun `perioderSomIkkeHarBlittVurdert - delvis vurdert gir gjenstående periode`() {
        val grunnlag = SamordningYtelseVurderingGrunnlag(
            ytelseGrunnlag = ytelse(periode = Periode(1 januar 2026, 31 januar 2026)),
            vurderingGrunnlag = vurdering(periode = Periode(15 januar 2026, 31 januar 2026)),
        )
        val ikkeVurdert = grunnlag.perioderSomIkkeHarBlittVurdert()
        assertThat(ikkeVurdert.segmenter()).hasSize(1)
        assertThat(ikkeVurdert.segmenter().first().periode).isEqualTo(Periode(1 januar 2026, 14 januar 2026))
    }
}