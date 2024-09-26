package no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class DefinisjonTest {

    @Test
    fun `Skal validere OK for alle definisjoner`() {
        try {
            Definisjon.entries.toTypedArray()
        } catch (e: Exception) {
            fail(e)
        }
    }
}
