package no.nav.aap.behandlingsflyt.dbconnect

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

internal class QueryTest {

    @Test
    fun `negative tester`() {
        skalKasteException(-1)
        skalKasteException(301)
    }

    @Test
    fun `positive tester`() {
        validering(100)
        validering(2)
        validering(1)
    }

    private fun skalKasteException(sekunder: Int) {
        try {
            validering(sekunder)
            fail("Skal kaste exception")
        } catch (exception: IllegalArgumentException) {
            // Expected
        }
    }
}