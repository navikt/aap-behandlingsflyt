package no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class AvklaringsbehovKodeTest {

    @Test
    fun `Skal hente ut kode basert på string`() {
        try {
            val kode = AvklaringsbehovKode.valueOf("5009")
            assertThat(kode).isEqualTo(AvklaringsbehovKode.`5009`)
        } catch (e: Exception) {
            fail(e)
        }
    }
}
