package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.flyt.behandlingstyper.Revurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BehandlingTypeTest {
    @Test
    fun name() {
        val utledType = utledType(Revurdering.identifikator())

        assertThat(utledType).isEqualTo(Revurdering)
    }
}