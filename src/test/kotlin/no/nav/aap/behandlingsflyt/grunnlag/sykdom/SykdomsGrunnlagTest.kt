package no.nav.aap.behandlingsflyt.grunnlag.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.NedreGrense
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.grunnlag.sykdom.SykdomsGrunnlag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykdomsGrunnlagTest {

    @Test
    fun `er konsistent hvis ikke yrkesskade og 50 prosent`() {
        val sykdomsGrunnlag = SykdomsGrunnlag(
            id = 1L,
            behandlingId = 1L,
            yrkesskadevurdering = null,
            sykdomsvurdering = Sykdomsvurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                erSkadeSykdomEllerLyteVesentligdel = true,
                erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                nedreGrense = NedreGrense.FEMTI,
                nedsattArbeidsevneDato = LocalDate.now()
            )
        )

        assertThat(sykdomsGrunnlag.erKonsistent()).isTrue
    }

    @Test
    fun `er ikke konsistent hvis ikke yrkesskade og 30 prosent`() {
        val sykdomsGrunnlag = SykdomsGrunnlag(
            id = 1L,
            behandlingId = 1L,
            yrkesskadevurdering = null,
            sykdomsvurdering = Sykdomsvurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                erSkadeSykdomEllerLyteVesentligdel = true,
                erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                nedreGrense = NedreGrense.TRETTI,
                nedsattArbeidsevneDato = LocalDate.now()
            )
        )

        assertThat(sykdomsGrunnlag.erKonsistent()).isFalse
    }

    @Test
    fun `er konsistent hvis yrkesskade med årsakssammenheng og 30 prosent`() {
        val sykdomsGrunnlag = SykdomsGrunnlag(
            id = 1L,
            behandlingId = 1L,
            yrkesskadevurdering = Yrkesskadevurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                erÅrsakssammenheng = true,
                skadetidspunkt = LocalDate.now()
            ),
            sykdomsvurdering = Sykdomsvurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                erSkadeSykdomEllerLyteVesentligdel = true,
                erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                nedreGrense = NedreGrense.TRETTI,
                nedsattArbeidsevneDato = LocalDate.now()
            )
        )

        assertThat(sykdomsGrunnlag.erKonsistent()).isTrue
    }

    @Test
    fun `er ikke konsistent hvis yrkesskade med årsakssammenheng og 50 prosent`() {
        val sykdomsGrunnlag = SykdomsGrunnlag(
            id = 1L,
            behandlingId = 1L,
            yrkesskadevurdering = Yrkesskadevurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                erÅrsakssammenheng = true,
                skadetidspunkt = LocalDate.now()
            ),
            sykdomsvurdering = Sykdomsvurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                erSkadeSykdomEllerLyteVesentligdel = true,
                erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                nedreGrense = NedreGrense.FEMTI,
                nedsattArbeidsevneDato = LocalDate.now()
            )
        )

        assertThat(sykdomsGrunnlag.erKonsistent()).isFalse
    }

    @Test
    fun `er ikke konsistent hvis yrkesskade uten årsakssammenheng og 30 prosent`() {
        val sykdomsGrunnlag = SykdomsGrunnlag(
            id = 1L,
            behandlingId = 1L,
            yrkesskadevurdering = Yrkesskadevurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                erÅrsakssammenheng = false,
                skadetidspunkt = LocalDate.now()
            ),
            sykdomsvurdering = Sykdomsvurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                erSkadeSykdomEllerLyteVesentligdel = true,
                erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                nedreGrense = NedreGrense.TRETTI,
                nedsattArbeidsevneDato = LocalDate.now()
            )
        )

        assertThat(sykdomsGrunnlag.erKonsistent()).isFalse
    }

    @Test
    fun `er konsistent hvis yrkesskade uten årsakssammenheng og 50 prosent`() {
        val sykdomsGrunnlag = SykdomsGrunnlag(
            id = 1L,
            behandlingId = 1L,
            yrkesskadevurdering = Yrkesskadevurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                erÅrsakssammenheng = false,
                skadetidspunkt = LocalDate.now()
            ),
            sykdomsvurdering = Sykdomsvurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                erSkadeSykdomEllerLyteVesentligdel = true,
                erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                nedreGrense = NedreGrense.FEMTI,
                nedsattArbeidsevneDato = LocalDate.now()
            )
        )

        assertThat(sykdomsGrunnlag.erKonsistent()).isTrue
    }
}