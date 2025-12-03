package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import java.time.LocalDate

class SykdomsvurderingTest {
    @Test
    fun `skal tolke svarene som oppfylt og ikke oppfylt`() {
        val vurdering = Sykdomsvurdering(
            begrunnelse = "",
            dokumenterBruktIVurdering = emptyList(),
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
            erArbeidsevnenNedsatt = true,
            yrkesskadeBegrunnelse = null,
            erNedsettelseIArbeidsevneAvEnVissVarighet = false,
            vurderingenGjelderFra = 1 januar 2020,
            vurderingenGjelderTil = null,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
            vurdertIBehandling = BehandlingId(1L)
        )

        assertThat(
            vurdering.erOppfyltOrdinær(
                LocalDate.now(),
                Periode(vurdering.vurderingenGjelderFra, Tid.MAKS)
            )
        ).isFalse
        assertThat(vurdering.erOppfyltOrdinærSettBortIfraVissVarighet()).isTrue
    }

    @ParameterizedTest
    @MethodSource("trueFalseNullSource")
    fun `skal ikke ta hensyn til viss varighet for vurderinger hvor kravdato er før gjelderFra`(
        erNedsettelseIArbeidsevneAvEnVissVarighet: Boolean?
    ) {
        // Parameterisert test. Skal ignorere verdien av erNedsettelseIArbeidsevneAvEnVissVarighet

        val gjelderFra = LocalDate.now()
        val kravdato = gjelderFra.plusMonths(1)

        val vurdering = Sykdomsvurdering(
            begrunnelse = "",
            dokumenterBruktIVurdering = emptyList(),
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
            erArbeidsevnenNedsatt = true,
            yrkesskadeBegrunnelse = null,
            erNedsettelseIArbeidsevneAvEnVissVarighet = erNedsettelseIArbeidsevneAvEnVissVarighet,
            vurderingenGjelderFra = kravdato.plusDays(1),
            vurderingenGjelderTil = null,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
            vurdertIBehandling = BehandlingId(1L)
        )

        assertThat(vurdering.erOppfyltOrdinær(kravdato, Periode(vurdering.vurderingenGjelderFra, Tid.MAKS))).isTrue
    }

    @Test
    fun `skal ta hensyn til viss varighet for revurdering når kravdato = gjelderFra`() {
        val gjelderFra = LocalDate.now()
        val vurdering = Sykdomsvurdering(
            begrunnelse = "",
            dokumenterBruktIVurdering = emptyList(),
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
            erArbeidsevnenNedsatt = true,
            yrkesskadeBegrunnelse = null,
            erNedsettelseIArbeidsevneAvEnVissVarighet = false,
            vurderingenGjelderFra = gjelderFra,
            vurderingenGjelderTil = null,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
            vurdertIBehandling = BehandlingId(1L)
        )

        assertThat(vurdering.erOppfyltOrdinær(gjelderFra, Periode(gjelderFra, Tid.MAKS))).isFalse
    }

    companion object {
        @JvmStatic
        private fun trueFalseNullSource() = arrayOf(
            true,
            false,
            null
        )
    }
}
