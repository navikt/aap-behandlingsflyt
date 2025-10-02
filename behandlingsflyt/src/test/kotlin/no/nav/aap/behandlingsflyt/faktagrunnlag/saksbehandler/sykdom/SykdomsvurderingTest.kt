package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.komponenter.verdityper.Bruker
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
            vurderingenGjelderFra = null,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
        )

        assertThat(vurdering.erOppfylt(LocalDate.now())).isFalse
        assertThat(vurdering.erOppfyltSettBortIfraVissVarighet()).isTrue
    }

    @ParameterizedTest
    @MethodSource("trueFalseNullSource")
    fun `skal ikke ta hensyn til viss varighet for revurdering`(erNedsettelseIArbeidsevneAvEnVissVarighet: Boolean?) {
        // Parameterisert test. Skal ignorere verdien av erNedsettelseIArbeidsevneAvEnVissVarighet

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
            erNedsettelseIArbeidsevneAvEnVissVarighet = erNedsettelseIArbeidsevneAvEnVissVarighet,
            vurderingenGjelderFra = gjelderFra,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
        )

        assertThat(vurdering.erOppfylt(gjelderFra.plusMonths(1))).isTrue
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
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
        )

        assertThat(vurdering.erOppfylt(gjelderFra)).isFalse
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