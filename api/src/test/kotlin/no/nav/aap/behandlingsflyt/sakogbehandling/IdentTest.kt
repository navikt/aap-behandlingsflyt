package no.nav.aap.behandlingsflyt.sakogbehandling

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IdentTest {
    @Test
    fun `er-funksjon fungerer som forventet`() {
        val ident1 = Ident("12345678901")
        val ident2 = Ident("12345678901")
        val ident3 = Ident("10987654321")

        assertTrue(ident1.er(ident2))
        assertFalse(ident1.er(ident3))
    }

    @Test
    fun `toString og maskert fungerer som forventet`() {
        val ident = Ident("12345678901")
        assertEquals("Ident(identifikator='123456*****')", ident.toString())
        assertEquals("123456*****", ident.maskert())
    }
}
