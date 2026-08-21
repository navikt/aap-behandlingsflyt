package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningYtelseVurderingGrunnlag
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SamordningYtelseGrunnlagTest {
    @Test
    fun `ytelser fra register med overlappende segmenter slås sammen ved sjekk av manglende vurderinger`() {
        val grunnlag = SamordningYtelseGrunnlag(
            1L,
            setOf(
                SamordningYtelse(
                    Ytelse.SYKEPENGER,
                    setOf(
                        SamordningYtelsePeriode(
                            Periode(1 januar 2024, 10 januar 2024),
                            Prosent.`70_PROSENT`,
                        ),
                        SamordningYtelsePeriode(
                            Periode(9 januar 2024, 13 januar 2024),
                            Prosent.`50_PROSENT`,
                        )
                    ),
                    kilde = "kilde"
                )
            ),
        )

        val ikkeVurdertePerioder =
            SamordningYtelseVurderingGrunnlag(grunnlag, null).perioderSomIkkeHarBlittVurdert(Periode(1 januar 2024, 31 januar 2024))

        assertThat(ikkeVurdertePerioder.segmenter().first().periode).isEqualTo(
            Periode(1 januar 2024, 13 januar 2024)
        )
    }
}