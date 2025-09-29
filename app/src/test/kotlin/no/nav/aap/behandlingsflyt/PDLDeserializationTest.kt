package no.nav.aap.behandlingsflyt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Endringstype
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Opplysningstype
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Personhendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.tilInnsending
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import org.junit.jupiter.api.assertNotNull
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PDLDeserializationTest {

    val objectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `deserialize AVDOED_PDL_V1 Personhendelse`() {
        val hendelseId = UUID.randomUUID().toString()
        val json = """
            {
                "hendelseId": "$hendelseId",
                "personidenter": ["01010112345"],
                "master": "FREG",
                "opprettet": "${Instant.now()}",
                "opplysningstype": "AVDOED_PDL_V1",
                "endringstype": "OPPRETTET",
                "navn": {
                    "fornavn": "Ola",
                    "etternavn": "Nordmann"
                }
            }
        """.trimIndent()

        val hendelse: Personhendelse = objectMapper.readValue(json)

        assertEquals(hendelseId, hendelse.hendelseId)
        assertEquals(listOf("01010112345"), hendelse.personidenter)
        assertEquals(Endringstype.OPPRETTET, hendelse.endringstype)
        assertEquals(Opplysningstype.AVDOED_PDL_V1, hendelse.opplysningstype)

        val navn = hendelse.navn
        assertNotNull(navn)
        assertEquals("Ola", navn.fornavn)
        assertEquals("Nordmann", navn.etternavn)

        // Optional: test tilInnsending mapping
        val innsending = hendelse.tilInnsending(Saksnummer("SAK123"))
        assertEquals("SAK123", innsending.saksnummer.toString())
        assertEquals(InnsendingType.PDL_HENDELSE, innsending.type)
    }
}