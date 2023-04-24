package no.nav.aap.domene.behandling.avklaringsbehov

import org.junit.jupiter.api.fail

class DefinisjonTest {

    @org.junit.jupiter.api.Test
    fun `Skal validere OK for alle definisjoner`() {
        try {
            Definisjon.values()
        } catch (e: Exception) {
            fail(e)
        }
    }
}
