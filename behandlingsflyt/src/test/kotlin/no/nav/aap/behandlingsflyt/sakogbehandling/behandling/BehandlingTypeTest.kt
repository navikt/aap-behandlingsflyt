package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class BehandlingTypeTest {
    @Test
    fun name() {
        val utledType = TypeBehandling.Companion.from(TypeBehandling.Revurdering.identifikator())

        Assertions.assertThat(utledType).isEqualTo(TypeBehandling.Revurdering)
    }
}