package no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.NedreGrense
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.beregning.Prosent
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.Beløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykdomsGrunnlagTest {

    @Test
    fun `er konsistent hvis ikke yrkesskade og 50 prosent`() {
        val sykdomGrunnlag = SykdomGrunnlag(
            id = 1L,
            yrkesskadevurdering = null,
            sykdomsvurdering = Sykdomsvurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                erSkadeSykdomEllerLyteVesentligdel = true,
                erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                nedreGrense = NedreGrense.FEMTI,
                nedsattArbeidsevneDato = LocalDate.now(),
                ytterligereNedsattArbeidsevneDato = null
            )
        )

        assertThat(sykdomGrunnlag.erKonsistent()).isTrue
    }

    @Test
    fun `er ikke konsistent hvis ikke yrkesskade og 30 prosent`() {
        val sykdomGrunnlag = SykdomGrunnlag(
            id = 1L,
            yrkesskadevurdering = null,
            sykdomsvurdering = Sykdomsvurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                erSkadeSykdomEllerLyteVesentligdel = true,
                erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                nedreGrense = NedreGrense.TRETTI,
                nedsattArbeidsevneDato = LocalDate.now(),
                ytterligereNedsattArbeidsevneDato = null
            )
        )

        assertThat(sykdomGrunnlag.erKonsistent()).isFalse
    }

    @Test
    fun `er konsistent hvis yrkesskade med årsakssammenheng og 30 prosent`() {
        val sykdomGrunnlag = SykdomGrunnlag(
            id = 1L,
            yrkesskadevurdering = Yrkesskadevurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                erÅrsakssammenheng = true,
                skadetidspunkt = LocalDate.now(),
                andelAvNedsettelse = Prosent.`100_PROSENT`,
                antattÅrligInntekt = Beløp(1000)
            ),
            sykdomsvurdering = Sykdomsvurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                erSkadeSykdomEllerLyteVesentligdel = true,
                erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                nedreGrense = NedreGrense.TRETTI,
                nedsattArbeidsevneDato = LocalDate.now(),
                ytterligereNedsattArbeidsevneDato = null
            )
        )

        assertThat(sykdomGrunnlag.erKonsistent()).isTrue
    }

    @Test
    fun `er ikke konsistent hvis yrkesskade med årsakssammenheng og 50 prosent`() {
        val sykdomGrunnlag = SykdomGrunnlag(
            id = 1L,
            yrkesskadevurdering = Yrkesskadevurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                erÅrsakssammenheng = true,
                skadetidspunkt = LocalDate.now(),
                andelAvNedsettelse = Prosent.`100_PROSENT`,
                antattÅrligInntekt = Beløp(1000)
            ),
            sykdomsvurdering = Sykdomsvurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                erSkadeSykdomEllerLyteVesentligdel = true,
                erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                nedreGrense = NedreGrense.FEMTI,
                nedsattArbeidsevneDato = LocalDate.now(),
                ytterligereNedsattArbeidsevneDato = null
            )
        )

        assertThat(sykdomGrunnlag.erKonsistent()).isFalse
    }

    @Test
    fun `er ikke konsistent hvis yrkesskade uten årsakssammenheng og 30 prosent`() {
        val sykdomGrunnlag = SykdomGrunnlag(
            id = 1L,
            yrkesskadevurdering = Yrkesskadevurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                erÅrsakssammenheng = false,
                skadetidspunkt = LocalDate.now(),
                andelAvNedsettelse = Prosent.`100_PROSENT`,
                antattÅrligInntekt = Beløp(1000)
            ),
            sykdomsvurdering = Sykdomsvurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                erSkadeSykdomEllerLyteVesentligdel = true,
                erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                nedreGrense = NedreGrense.TRETTI,
                nedsattArbeidsevneDato = LocalDate.now(),
                ytterligereNedsattArbeidsevneDato = null
            )
        )

        assertThat(sykdomGrunnlag.erKonsistent()).isFalse
    }

    @Test
    fun `er konsistent hvis yrkesskade uten årsakssammenheng og 50 prosent`() {
        val sykdomGrunnlag = SykdomGrunnlag(
            id = 1L,
            yrkesskadevurdering = Yrkesskadevurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                erÅrsakssammenheng = false,
                skadetidspunkt = LocalDate.now(),
                andelAvNedsettelse = Prosent.`100_PROSENT`,
                antattÅrligInntekt = Beløp(1000)
            ),
            sykdomsvurdering = Sykdomsvurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                erSkadeSykdomEllerLyteVesentligdel = true,
                erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                nedreGrense = NedreGrense.FEMTI,
                nedsattArbeidsevneDato = LocalDate.now(),
                ytterligereNedsattArbeidsevneDato = null
            )
        )

        assertThat(sykdomGrunnlag.erKonsistent()).isTrue
    }
}