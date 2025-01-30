package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

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
        )

        assertThat(vurdering.erOppfylt()).isFalse
        assertThat(vurdering.erOppfyltSettBortIfraVissVarighet()).isTrue
    }
}