package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.komponenter.httpklient.auth.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

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

        assertThat(vurdering.erOppfylt()).isFalse
        assertThat(vurdering.erOppfyltSettBortIfraVissVarighet()).isTrue
    }
}