package no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling.Revurdering
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class SykdomsInformasjonskravTest {

    @Test
    fun `er konsistent hvis ikke yrkesskade og 50 prosent`() {
        val vurdering = Sykdomsvurdering(
            begrunnelse = "",
            dokumenterBruktIVurdering = emptyList(),
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
            erArbeidsevnenNedsatt = true,
            yrkesskadeBegrunnelse = null,
            vurderingenGjelderFra = null,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
        )

        assertThat(vurdering.erKonsistentForSykdom(false, Førstegangsbehandling)).isTrue
    }

    @Test
    fun `er konsistent hvis yrkesskade med årsakssammenheng og 30 prosent`() {
        val vurdering = Sykdomsvurdering(
            begrunnelse = "",
            dokumenterBruktIVurdering = emptyList(),
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
            erArbeidsevnenNedsatt = true,
            yrkesskadeBegrunnelse = null,
            vurderingenGjelderFra = null,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
        )

        assertThat(vurdering.erKonsistentForSykdom(true, Førstegangsbehandling)).isTrue
    }

    @Test
    fun `er ikke konsistent hvis yrkesskade uten årsakssammenheng og 30 prosent`() {
        val vurdering = Sykdomsvurdering(
            begrunnelse = "",
            dokumenterBruktIVurdering = emptyList(),
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = false,
            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
            erArbeidsevnenNedsatt = true,
            yrkesskadeBegrunnelse = null,
            vurderingenGjelderFra = null,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
        )

        assertThat(vurdering.erKonsistentForSykdom(true, Førstegangsbehandling)).isFalse
    }

    @Test
    fun `er ikke konsistent hvis yrkesskade 30 prosent og ingen begrunnelse for ys`() {
        val vurdering = Sykdomsvurdering(
            begrunnelse = "",
            dokumenterBruktIVurdering = emptyList(),
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = false,
            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true,
            erArbeidsevnenNedsatt = true,
            yrkesskadeBegrunnelse = null,
            vurderingenGjelderFra = null,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
        )

        assertThat(vurdering.erKonsistentForSykdom(true, Førstegangsbehandling)).isFalse
    }

    @Test
    fun `er konsistent hvis yrkesskade 30 prosent og ingen begrunnelse for ys på revurdering`() {
        val vurdering = Sykdomsvurdering(
            begrunnelse = "",
            dokumenterBruktIVurdering = emptyList(),
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = false,
            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true,
            erArbeidsevnenNedsatt = true,
            yrkesskadeBegrunnelse = null,
            vurderingenGjelderFra = null,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
        )

        assertThat(vurdering.erKonsistentForSykdom(true, Revurdering)).isTrue
    }

    @Test
    fun `er konsistent hvis yrkesskade uten årsakssammenheng og 50 prosent`() {
        val vurdering = Sykdomsvurdering(
            begrunnelse = "",
            dokumenterBruktIVurdering = emptyList(),
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
            erArbeidsevnenNedsatt = true,
            yrkesskadeBegrunnelse = null,
            vurderingenGjelderFra = null,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
        )

        assertThat(vurdering.erKonsistentForSykdom(true, Førstegangsbehandling)).isTrue
    }

    @Test
    fun `er konsistent hvis ikke ssl og ssl ikke vesentlig del`() {
        val vurdering = Sykdomsvurdering(
            begrunnelse = "",
            dokumenterBruktIVurdering = emptyList(),
            harSkadeSykdomEllerLyte = false,
            erSkadeSykdomEllerLyteVesentligdel = false,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
            erArbeidsevnenNedsatt = true,
            yrkesskadeBegrunnelse = null,
            vurderingenGjelderFra = null,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
        )

        assertThat(vurdering.erKonsistentForSykdom(false, Førstegangsbehandling)).isTrue
    }

    @Test
    fun `er konsistent hvis ssl og ssl ikke vesentlig del`() {
        val vurdering = Sykdomsvurdering(
            begrunnelse = "",
            dokumenterBruktIVurdering = emptyList(),
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = false,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
            erArbeidsevnenNedsatt = true,
            yrkesskadeBegrunnelse = null,
            vurderingenGjelderFra = null,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
        )

        assertThat(vurdering.erKonsistentForSykdom(false, Førstegangsbehandling)).isTrue
    }

    @Test
    fun `er konsistent hvis ssl og ssl vesentlig del`() {
        val vurdering = Sykdomsvurdering(
            begrunnelse = "",
            dokumenterBruktIVurdering = emptyList(),
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
            erArbeidsevnenNedsatt = true,
            yrkesskadeBegrunnelse = null,
            vurderingenGjelderFra = null,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
        )

        assertThat(vurdering.erKonsistentForSykdom(false, Førstegangsbehandling)).isTrue
    }

    @Test
    fun `er ikke konsistend hvis ikke ssl og ssl vesentlig del`() {
        val vurdering = Sykdomsvurdering(
            begrunnelse = "",
            dokumenterBruktIVurdering = emptyList(),
            harSkadeSykdomEllerLyte = false,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
            erArbeidsevnenNedsatt = true,
            yrkesskadeBegrunnelse = null,
            vurderingenGjelderFra = null,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
        )

        assertThat(vurdering.erKonsistentForSykdom(false, Førstegangsbehandling)).isFalse
    }
}