package no.nav.aap.behandlingsflyt

import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SystembrukerTest {

    @Test
    fun `Kelvin og ArenaMigrering er systembrukere`() {
        assertThat(SYSTEMBRUKER.erSystembruker()).isTrue()
        assertThat(ARENA_MIGRERING_BRUKER.erSystembruker()).isTrue()
    }

    @Test
    fun `saksbehandler er ikke systembruker`() {
        assertThat(Bruker("Z123456").erSystembruker()).isFalse()
    }
}
