package no.nav.aap.behandlingsflyt.utils

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import javax.crypto.AEADBadTagException

class KryptertStringTest {

    private val gyldigNøkkel = "en-hemmelig-nokkel-pa-32-bytes!!".toByteArray(Charsets.UTF_8)
    private val codec = KryptertString(gyldigNøkkel)

    private val testVerdi = "12345678901"

    // --- encode ---

    @Test
    fun `encode returnerer en ikke-tom streng`() {
        val kelvinId = codec.encode(testVerdi)
        assertThat(kelvinId).isNotBlank()
    }

    @Test
    fun `encode returnerer ulik verdi for hvert kall selv med samme ident`() {
        val kelvinId1 = codec.encode(testVerdi)
        val kelvinId2 = codec.encode(testVerdi)
        val kelvinId3 = codec.encode(testVerdi)

        assertThat(kelvinId1).isNotEqualTo(kelvinId2)
        assertThat(kelvinId2).isNotEqualTo(kelvinId3)
        assertThat(kelvinId1).isNotEqualTo(kelvinId3)
    }

    // --- decode ---

    @Test
    fun `decode returnerer riktig identifikator etter encode`() {
        val kelvinId = codec.encode(testVerdi)
        val decoded = codec.decode(kelvinId)

        assertThat(decoded).isEqualTo(testVerdi)
    }

    @Test
    fun `decode er stabil - flere kall med samme kelvinId returnerer samme identifikator`() {
        val kelvinId = codec.encode(testVerdi)

        assertThat(codec.decode(kelvinId)).isEqualTo(codec.decode(kelvinId))
    }

    @Test
    fun `decode fungerer for ulike identifikatorer`() {
        val identer = listOf("12345678901", "98765432109", "00000000000")

        identer.forEach { id ->
            assertThat(codec.decode(codec.encode(id))).isEqualTo(id)
        }
    }

    // --- feil: manipulering ---

    @Test
    fun `decode kaster AEADBadTagException ved manipulert krypteringstekst`() {
        val kelvinId = codec.encode(testVerdi)
        val bytes = java.util.Base64.getUrlDecoder().decode(kelvinId).also { it[15] = (it[15].toInt() xor 0xFF).toByte() }
        val manipulert = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

        assertThatThrownBy { codec.decode(manipulert) }
            .isInstanceOf(AEADBadTagException::class.java)
    }

    @Test
    fun `decode kaster AEADBadTagException ved manipulert nonce`() {
        val kelvinId = codec.encode(testVerdi)
        val bytes = java.util.Base64.getUrlDecoder().decode(kelvinId).also { it[0] = (it[0].toInt() xor 0xFF).toByte() }
        val manipulert = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

        assertThatThrownBy { codec.decode(manipulert) }
            .isInstanceOf(AEADBadTagException::class.java)
    }

    // --- feil: feil nøkkel ---

    @Test
    fun `decode kaster AEADBadTagException når man bruker feil nøkkel`() {
        val kelvinId = codec.encode(testVerdi)
        val annenNøkkel = "en-annen-hemmelig-nokkel-32bytes".toByteArray(Charsets.UTF_8)
        val annenCodec = KryptertString(annenNøkkel)

        assertThatThrownBy { annenCodec.decode(kelvinId) }
            .isInstanceOf(AEADBadTagException::class.java)
    }

    // --- feil: ugyldig input ---

    @Test
    fun `decode kaster IllegalArgumentException ved ugyldig base64`() {
        assertThatThrownBy { codec.decode("ikke-gyldig-base64!!!") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Invalid kelvinId format")
    }

    @Test
    fun `decode kaster IllegalArgumentException ved for kort input`() {
        val forKort = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(4))

        assertThatThrownBy { codec.decode(forKort) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Invalid kelvinId length")
    }

    // --- konstruktør ---

    @Test
    fun `konstruktør kaster IllegalArgumentException når nøkkel ikke er 32 bytes`() {
        assertThatThrownBy { KryptertString("for-kort".toByteArray()) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("AES-256 requires a 32-byte key")

        assertThatThrownBy { KryptertString(ByteArray(64)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("AES-256 requires a 32-byte key")
    }
}
