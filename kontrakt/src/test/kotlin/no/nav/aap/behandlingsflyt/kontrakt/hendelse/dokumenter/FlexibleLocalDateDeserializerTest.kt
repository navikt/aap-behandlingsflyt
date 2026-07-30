package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class FlexibleLocalDateDeserializerTest {

    private val mapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(
            SimpleModule().addDeserializer(LocalDate::class.java, FlexibleLocalDateDeserializer())
        )

    private data class Payload(val dato: LocalDate)

    @Test
    fun `deserializes ISO_LOCAL_DATE`() {
        val json = """{"dato":"2026-06-24"}"""

        val result = mapper.readValue(json, Payload::class.java)

        assertEquals(LocalDate.of(2026, 6, 24), result.dato)
    }

    @Test
    fun `deserializes dd MM yyyy`() {
        val json = """{"dato":"24.06.2026"}"""

        val result = mapper.readValue(json, Payload::class.java)

        assertEquals(LocalDate.of(2026, 6, 24), result.dato)
    }

    @Test
    fun `trims surrounding whitespace`() {
        val json = """{"dato":" 24.06.2026 "}"""

        val result = mapper.readValue(json, Payload::class.java)

        assertEquals(LocalDate.of(2026, 6, 24), result.dato)
    }

    @Test
    fun `fails on empty string`() {
        val json = """{"dato":""}"""

        assertThrows<JsonMappingException> {
            mapper.readValue(json, Payload::class.java)
        }
    }

    @Test
    fun `fails on unsupported format with clear message`() {
        val json = """{"dato":"24/06/2026"}"""

        val ex = assertThrows<InvalidFormatException> {
            mapper.readValue(json, Payload::class.java)
        }

        assertTrue(ex.message.orEmpty().contains("yyyy-MM-dd or dd.MM.yyyy"))
    }

    @Test
    fun `fails on invalid date`() {
        val json = """{"dato":"2026-13-01"}"""

        val ex = assertThrows<JsonMappingException> {
            mapper.readValue(json, Payload::class.java)
        }

        assertTrue(ex.message.orEmpty().contains("yyyy-MM-dd or dd.MM.yyyy"))
    }
}