package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.ARENA_MIGRERING_BRUKER
import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurderingKilde
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.test.januar
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VurdertAvResponseTest {

    @Test
    fun `kilde utledes fra ident`() {
        assertThat(response(ARENA_MIGRERING_BRUKER.ident).kilde).isEqualTo(VurderingKilde.MIGRERT_FRA_ARENA)
        assertThat(response(SYSTEMBRUKER.ident).kilde).isEqualTo(VurderingKilde.AUTOMATISK)
        assertThat(response("Z123456").kilde).isEqualTo(VurderingKilde.SAKSBEHANDLER)
    }

    @Test
    fun `kilde beholdes ved copy`() {
        val kopi = response(ARENA_MIGRERING_BRUKER.ident).copy(erRetur = true)
        assertThat(kopi.kilde).isEqualTo(VurderingKilde.MIGRERT_FRA_ARENA)
    }

    private fun response(ident: String) = VurdertAvResponse(ident = ident, dato = 1 januar 2026)
}
