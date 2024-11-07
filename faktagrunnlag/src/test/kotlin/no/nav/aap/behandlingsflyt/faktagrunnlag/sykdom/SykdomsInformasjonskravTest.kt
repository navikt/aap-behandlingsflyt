package no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykdomsInformasjonskravTest {

    @Test
    fun `er konsistent hvis ikke yrkesskade og 50 prosent`() {
        val sykdomGrunnlag = SykdomGrunnlag(
            id = 1L,
            yrkesskadevurdering = null,
            sykdomsvurdering = Sykdomsvurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                harSkadeSykdomEllerLyte = true,
                erSkadeSykdomEllerLyteVesentligdel = true,
                erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                erArbeidsevnenNedsatt = true,
            )
        )

        assertThat(sykdomGrunnlag.erKonsistentForSykdom()).isTrue
    }

    @Test
    fun `er ikke konsistent hvis ikke yrkesskade og 30 prosent`() {
        val sykdomGrunnlag = SykdomGrunnlag(
            id = 1L,
            yrkesskadevurdering = null,
            sykdomsvurdering = Sykdomsvurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                harSkadeSykdomEllerLyte = true,
                erSkadeSykdomEllerLyteVesentligdel = true,
                erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                erArbeidsevnenNedsatt = true,
            )
        )

        assertThat(sykdomGrunnlag.erKonsistentForSykdom()).isFalse
    }

    @Test
    fun `er konsistent hvis yrkesskade med årsakssammenheng og 30 prosent`() {
        val sykdomGrunnlag = SykdomGrunnlag(
            id = 1L,
            yrkesskadevurdering = Yrkesskadevurdering(
                begrunnelse = "",
                erÅrsakssammenheng = true,
                skadetidspunkt = LocalDate.now(),
                andelAvNedsettelse = Prosent.`100_PROSENT`,
            ),
            sykdomsvurdering = Sykdomsvurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                harSkadeSykdomEllerLyte = true,
                erSkadeSykdomEllerLyteVesentligdel = true,
                erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                erArbeidsevnenNedsatt = true,
            )
        )

        assertThat(sykdomGrunnlag.erKonsistentForSykdom()).isTrue
    }

    @Test
    fun `er ikke konsistent hvis yrkesskade med årsakssammenheng og 50 prosent`() {
        val sykdomGrunnlag = SykdomGrunnlag(
            id = 1L,
            yrkesskadevurdering = Yrkesskadevurdering(
                begrunnelse = "",
                erÅrsakssammenheng = true,
                skadetidspunkt = LocalDate.now(),
                andelAvNedsettelse = Prosent.`100_PROSENT`,
            ),
            sykdomsvurdering = Sykdomsvurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                harSkadeSykdomEllerLyte = true,
                erSkadeSykdomEllerLyteVesentligdel = true,
                erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                erArbeidsevnenNedsatt = true,
            )
        )

        assertThat(sykdomGrunnlag.erKonsistentForSykdom()).isFalse
    }

    @Test
    fun `er ikke konsistent hvis yrkesskade uten årsakssammenheng og 30 prosent`() {
        val sykdomGrunnlag = SykdomGrunnlag(
            id = 1L,
            yrkesskadevurdering = Yrkesskadevurdering(
                begrunnelse = "",
                erÅrsakssammenheng = false,
                skadetidspunkt = LocalDate.now(),
                andelAvNedsettelse = Prosent.`100_PROSENT`,
            ),
            sykdomsvurdering = Sykdomsvurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                harSkadeSykdomEllerLyte = true,
                erSkadeSykdomEllerLyteVesentligdel = true,
                erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                erArbeidsevnenNedsatt = true,
            )
        )

        assertThat(sykdomGrunnlag.erKonsistentForSykdom()).isFalse
    }

    @Test
    fun `er konsistent hvis yrkesskade uten årsakssammenheng og 50 prosent`() {
        val sykdomGrunnlag = SykdomGrunnlag(
            id = 1L,
            yrkesskadevurdering = Yrkesskadevurdering(
                begrunnelse = "",
                erÅrsakssammenheng = false,
                skadetidspunkt = LocalDate.now(),
                andelAvNedsettelse = Prosent.`100_PROSENT`,
            ),
            sykdomsvurdering = Sykdomsvurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                harSkadeSykdomEllerLyte = true,
                erSkadeSykdomEllerLyteVesentligdel = true,
                erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                erArbeidsevnenNedsatt = true,
            )
        )

        assertThat(sykdomGrunnlag.erKonsistentForSykdom()).isTrue
    }

    @Test
    fun `er konsistent hvis ikke ssl og ssl ikke vesentlig del`() {
        val sykdomGrunnlag = SykdomGrunnlag(
            id = 1L,
            yrkesskadevurdering = null,
            sykdomsvurdering = Sykdomsvurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                harSkadeSykdomEllerLyte = false,
                erSkadeSykdomEllerLyteVesentligdel = false,
                erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                erArbeidsevnenNedsatt = true,
            )
        )

        assertThat(sykdomGrunnlag.erKonsistentForSykdom()).isTrue
    }

    @Test
    fun `er konsistent hvis ssl og ssl ikke vesentlig del`() {
        val sykdomGrunnlag = SykdomGrunnlag(
            id = 1L,
            yrkesskadevurdering = null,
            sykdomsvurdering = Sykdomsvurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                harSkadeSykdomEllerLyte = true,
                erSkadeSykdomEllerLyteVesentligdel = false,
                erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                erArbeidsevnenNedsatt = true,
            )
        )

        assertThat(sykdomGrunnlag.erKonsistentForSykdom()).isTrue
    }

    @Test
    fun `er konsistent hvis ssl og ssl vesentlig del`() {
        val sykdomGrunnlag = SykdomGrunnlag(
            id = 1L,
            yrkesskadevurdering = null,
            sykdomsvurdering = Sykdomsvurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                harSkadeSykdomEllerLyte = true,
                erSkadeSykdomEllerLyteVesentligdel = true,
                erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                erArbeidsevnenNedsatt = true,
            )
        )

        assertThat(sykdomGrunnlag.erKonsistentForSykdom()).isTrue
    }

    @Test
    fun `er ikke konsistend hvis ikke ssl og ssl vesentlig del`() {
        val sykdomGrunnlag = SykdomGrunnlag(
            id = 1L,
            yrkesskadevurdering = null,
            sykdomsvurdering = Sykdomsvurdering(
                begrunnelse = "",
                dokumenterBruktIVurdering = emptyList(),
                harSkadeSykdomEllerLyte = false,
                erSkadeSykdomEllerLyteVesentligdel = true,
                erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                erArbeidsevnenNedsatt = true,
            )
        )

        assertThat(sykdomGrunnlag.erKonsistentForSykdom()).isFalse
    }
}