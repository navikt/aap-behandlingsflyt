package no.nav.aap.behandlingsflyt.kontrakt.sak

import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

class SaksnummerTest {

    @Test
    fun `valueOf skal normalisere strengen - uten O and I`() {
        val saksnummer = Saksnummer.valueOf(123456L)
        val str = saksnummer.toString()
        assertFalse(str.contains('O'), "Should not contain uppercase O")
        assertFalse(str.contains('I'), "Should not contain uppercase I")
    }

    @Test
    fun `valueOf er deterministisk for samme id`() {
        repeat(10) {
            val id = Random.nextLong(1000000, 9000000)
            assertEquals(Saksnummer.valueOf(id), Saksnummer.valueOf(id))
        }
    }

    @Test
    fun `fra normaliserer saksnummeret`() {
        val saksnummer = Saksnummer.fra("abc0oi")
        val str = saksnummer.toString()
        assertFalse(str.contains('O'), "Should not contain uppercase O")
        assertFalse(str.contains('I'), "Should not contain uppercase I")
        assertEquals("ABC0oi", str)
    }

    @Test
    fun `fra-verdi som allerede er normalisert, skal beholde lowercase o and i`() {
        val saksnummer = Saksnummer.fra("AOiBZ")
        val str = saksnummer.toString()
        assertFalse(str.contains('O'))
        assertFalse(str.contains('I'))
        assertTrue(str.contains('o'))
        assertTrue(str.contains('i'))
    }

    @Test
    fun `equals fungerer på samme identifikator`() {
        val a = Saksnummer.fra("abc123")
        val b = Saksnummer.fra("abc123")
        assertEquals(a, b)
    }

    @Test
    fun `equals fungerer på ulik identifikator`() {
        val a = Saksnummer.fra("abc123")
        val b = Saksnummer.fra("xyz789")
        assertNotEquals(a, b)
    }

    @Test
    fun `hashCode er konsistent med equals`() {
        val a = Saksnummer.fra("abc123")
        val b = Saksnummer.fra("abc123")
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `fra-verdi normaliserer strengen slik at fra og valueOf gir samme resultat`() {
        val fromLower = Saksnummer.fra("1bij53o0")
        val fromUpper = Saksnummer.fra("1BIJ53O0")
        assertEquals(fromLower, fromUpper)
    }

    @Test
    fun `fra-verdi trimmer strengen`() {
        val fromLower = Saksnummer.fra("1bij53o0    ")
        val fromUpper = Saksnummer.fra("    1BIJ53O0")
        assertEquals(fromLower, fromUpper)
    }

    @Test
    fun `toString gir identifikatoren`() {
        val saksnummer = Saksnummer.fra("test123")
        assertEquals("TEST123", saksnummer.toString())
    }

    @Test
    fun `sjekk at serialisering og deserialisering fungerer som forventet`() {
        val saksnummer = Saksnummer.fra("1bij53o0")

        val serialized = DefaultJsonMapper.toJson(saksnummer)
        assertEquals("\"1BiJ53o0\"", serialized)

        val deserialized = DefaultJsonMapper.fromJson<Saksnummer>(serialized.lowercase())

        assertEquals("1BiJ53o0", deserialized.toString())
        assertEquals(saksnummer, deserialized)
    }
}