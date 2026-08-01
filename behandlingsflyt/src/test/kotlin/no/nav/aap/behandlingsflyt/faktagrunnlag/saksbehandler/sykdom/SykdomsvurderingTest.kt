package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.sykdom.Sykdomsvurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class SykdomsvurderingTest {
    @Test
    fun `skal tolke svarene som oppfylt og ikke oppfylt`() {
        val vurdering = Sykdomsvurdering(
            begrunnelse = "",
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
            harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA_FORBIGÅENDE_PROBLEMER,
            yrkesskadeBegrunnelse = null,
            vurderingenGjelderFra = 1 januar 2020,
            vurderingenGjelderTil = null,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
            vurdertIBehandling = BehandlingId(1L),
            diagnose = null,
        )

        assertThat(
            vurdering.erOppfyltOrdinærMedUtlededeFelter()
        ).isFalse
        
        assertThat(
            vurdering.skalVurderesForSykepengeerstatning()
        ).isTrue
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
