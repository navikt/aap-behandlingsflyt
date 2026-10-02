package no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomsvurderingFeil
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class SykdomsInformasjonskravTest {

    @Test
    fun `er konsistent hvis ikke yrkesskade og 50 prosent`() {
        val vurdering = Sykdomsvurdering(
            begrunnelse = "",
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
            harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
            yrkesskadeBegrunnelse = null,
            vurderingenGjelderFra = 1 januar 2020,
            vurderingenGjelderTil = null,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
            vurdertIBehandling = BehandlingId(1L),
            diagnose = null
        )

        assertThat(vurdering.validerKonsistensForSykdom(true)).isEmpty()
    }

    @Test
    fun `er konsistent hvis yrkesskade med årsakssammenheng og 30 prosent`() {
        val vurdering = Sykdomsvurdering(
            begrunnelse = "",
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
            harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
            yrkesskadeBegrunnelse = null,
            vurderingenGjelderFra = 1 januar 2020,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
            vurdertIBehandling = BehandlingId(1L),
            vurderingenGjelderTil = null,
            diagnose = null
        )

        assertThat(vurdering.validerKonsistensForSykdom(true))
            .isEmpty()
    }

    @Test
    fun `er ikke konsistent hvis yrkesskade uten årsakssammenheng og 30 prosent`() {
        val vurdering = Sykdomsvurdering(
            begrunnelse = "",
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = false,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
            harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
            yrkesskadeBegrunnelse = null,
            vurderingenGjelderFra = 1 januar 2020,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
            vurdertIBehandling = BehandlingId(1L),
            vurderingenGjelderTil = null,
            diagnose = null
        )

        assertThat(vurdering.validerKonsistensForSykdom(true))
            .containsExactly(SykdomsvurderingFeil.MANGLER_VURDERING_AV_YRKESSKADEGRENSE)
    }

    @Test
    fun `er konsistent hvis yrkesskade 30 prosent og ingen begrunnelse for ys på revurdering`() {
        val vurdering = Sykdomsvurdering(
            begrunnelse = "",
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = false,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true,
            harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
            yrkesskadeBegrunnelse = null,
            vurderingenGjelderFra = 1 januar 2020,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
            vurdertIBehandling = BehandlingId(1L),
            vurderingenGjelderTil = null,
            diagnose = null
        )

        assertThat(vurdering.validerKonsistensForSykdom(true))
            .isEmpty()
    }

    @Test
    fun `er konsistent hvis yrkesskade uten årsakssammenheng og 50 prosent`() {
        val vurdering = Sykdomsvurdering(
            begrunnelse = "",
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
            harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
            yrkesskadeBegrunnelse = null,
            vurderingenGjelderFra = 1 januar 2020,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
            vurdertIBehandling = BehandlingId(1L),
            vurderingenGjelderTil = null,
            diagnose = null
        )

        assertThat(vurdering.validerKonsistensForSykdom(true))
            .isEmpty()
    }

    @Test
    fun `er konsistent hvis ikke sykdom, skade, lyte og sykdom, skade, lyte ikke vesentlig del`() {
        val vurdering = Sykdomsvurdering(
            begrunnelse = "",
            harSkadeSykdomEllerLyte = false,
            erSkadeSykdomEllerLyteVesentligdel = false,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
            harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
            yrkesskadeBegrunnelse = null,
            vurderingenGjelderFra = 1 januar 2020,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
            vurdertIBehandling = BehandlingId(1L),
            vurderingenGjelderTil = null,
            diagnose = null
        )

        assertThat(vurdering.validerKonsistensForSykdom(true))
            .isEmpty()
    }

    @Test
    fun `er konsistent hvis sykdom, skade, lyte og sykdom, skade, lyte ikke vesentlig del`() {
        val vurdering = Sykdomsvurdering(
            begrunnelse = "",
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = false,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
            harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
            yrkesskadeBegrunnelse = null,
            vurderingenGjelderFra = 1 januar 2020,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
            vurdertIBehandling = BehandlingId(1L),
            vurderingenGjelderTil = null,
            diagnose = null
        )

        assertThat(vurdering.validerKonsistensForSykdom(true))
            .isEmpty()
    }

    @Test
    fun `er konsistent hvis sykdom, skade, lyte og sykdom, skade, lyte vesentlig del`() {
        val vurdering = Sykdomsvurdering(
            begrunnelse = "",
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
            harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
            yrkesskadeBegrunnelse = null,
            vurderingenGjelderFra = 1 januar 2020,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
            vurdertIBehandling = BehandlingId(1L),
            vurderingenGjelderTil = null,
            diagnose = null
        )

        assertThat(vurdering.validerKonsistensForSykdom(true))
            .isEmpty()
    }

    @Test
    fun `er konsistens hvis ikke sykdom, skade, lyte og sykdom, skade, lyte vesentlig del`() {
        val vurdering = Sykdomsvurdering(
            begrunnelse = "",
            harSkadeSykdomEllerLyte = false,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
            harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
            yrkesskadeBegrunnelse = null,
            vurderingenGjelderFra = 1 januar 2020,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
            vurdertIBehandling = BehandlingId(1L),
            vurderingenGjelderTil = null,
            diagnose = null
        )

        assertThat(vurdering.validerKonsistensForSykdom(true))
            .isEmpty()
    }

    @Test
    fun `gir feil når nedsatt arbeidsevne mangler`() {
        val vurdering = lagVurdering(harNedsattArbeidsevne = null)

        assertThat(vurdering.validerKonsistensForSykdom(false))
            .containsExactly(SykdomsvurderingFeil.MANGLER_NEDSATT_ARBEIDSEVNE)
    }

    @Test
    fun `gir feil når nei til nedsatt arbeidsevne motsies av 50-prosentvurderingen`() {
        val vurdering = lagVurdering(
            harNedsattArbeidsevne = ArbeidsevneNedsattValg.NEI,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
        )

        assertThat(vurdering.validerKonsistensForSykdom(false))
            .containsExactly(SykdomsvurderingFeil.NEDSATT_ARBEIDSEVNE_STEMMER_IKKE_MED_50_PROSENT)
    }

    @Test
    fun `gir feil når nei til nedsatt arbeidsevne motsies av vesentlighetsvurderingen`() {
        val vurdering = lagVurdering(
            harNedsattArbeidsevne = ArbeidsevneNedsattValg.NEI,
            erSkadeSykdomEllerLyteVesentligdel = true,
        )

        assertThat(vurdering.validerKonsistensForSykdom(false))
            .containsExactly(SykdomsvurderingFeil.NEDSATT_ARBEIDSEVNE_STEMMER_IKKE_MED_VESENTLIGHET)
    }

    private fun lagVurdering(
        harNedsattArbeidsevne: ArbeidsevneNedsattValg? = ArbeidsevneNedsattValg.JA,
        erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean? = false,
        erSkadeSykdomEllerLyteVesentligdel: Boolean? = false,
    ): Sykdomsvurdering = Sykdomsvurdering(
        begrunnelse = "",
        vurderingenGjelderFra = 1 januar 2020,
        vurderingenGjelderTil = null,
        harSkadeSykdomEllerLyte = true,
        erSkadeSykdomEllerLyteVesentligdel = erSkadeSykdomEllerLyteVesentligdel,
        erNedsettelseIArbeidsevneMerEnnHalvparten = erNedsettelseIArbeidsevneMerEnnHalvparten,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
        yrkesskadeBegrunnelse = null,
        harNedsattArbeidsevne = harNedsattArbeidsevne,
        diagnose = null,
        vurdertAv = Bruker("Z00000"),
        vurdertIBehandling = BehandlingId(1L),
        opprettet = Instant.now(),
    )
}