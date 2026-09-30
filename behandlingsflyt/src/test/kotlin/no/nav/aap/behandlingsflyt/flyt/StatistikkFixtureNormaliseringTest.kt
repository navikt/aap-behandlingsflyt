package no.nav.aap.behandlingsflyt.flyt

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class StatistikkFixtureNormaliseringTest {
    private val mapper = ObjectMapper()

    @Test
    fun `gir likt resultat ved ulike genererte verdier og bevarer referanser og rekkefølge`() {
        val første = mapper.readTree(
            """{"behandlingReferanse":"c73524df-05e4-4b44-8496-813fd45046ea","relatertBehandling":"c73524df-05e4-4b44-8496-813fd45046ea","ident":"12345678901","identerForSak":["12345678901"],"saksnummer":"4A1B2C3","hendelsesTidspunkt":"2026-09-30T10:10:03","avklaringsbehov":[{"tidsstempel":"2026-09-30T10:10:01"},{"tidsstempel":"2026-09-30T10:10:02"}],"fraDato":"2026-09-29"}"""
        )
        val andre = mapper.readTree(
            """{"behandlingReferanse":"f93e8a47-4ba2-4190-b117-2d9b7b5b56a2","relatertBehandling":"f93e8a47-4ba2-4190-b117-2d9b7b5b56a2","ident":"09876543210","identerForSak":["09876543210"],"saksnummer":"4D4E5F6","hendelsesTidspunkt":"2026-10-01T11:30:03","avklaringsbehov":[{"tidsstempel":"2026-10-01T11:30:01"},{"tidsstempel":"2026-10-01T11:30:02"}],"fraDato":"2026-09-30"}"""
        )

        val forventet = normaliserStatistikkFixture(første, LocalDate.parse("2026-09-30"))
        val faktisk = normaliserStatistikkFixture(andre, LocalDate.parse("2026-10-01"))

        assertEquals(forventet, faktisk)
        assertEquals(forventet["behandlingReferanse"], forventet["relatertBehandling"])
        assertEquals(forventet["ident"], forventet["identerForSak"][0])
        assertEquals("2025-09-23", forventet["fraDato"].asText())
    }

    @Test
    fun `vedtakstidspunkt etter mottatt tid uten avklaringsbehov`() {
        val hendelse = mapper.readTree(
            """{"mottattTid":"2026-09-30T10:10:00","avsluttetBehandling":{"vedtakstidspunkt":"2026-09-30T10:10:01"}}"""
        )

        val normalisert = normaliserStatistikkFixture(hendelse, LocalDate.parse("2026-09-30"))

        assertEquals("2025-09-24T12:00", normalisert["mottattTid"].asText())
        assertEquals("2025-09-24T12:00:00.001", normalisert["avsluttetBehandling"]["vedtakstidspunkt"].asText())
    }

    @Test
    fun `formaterer json som IntelliJ med fire mellomrom og linjeskift i lister`() {
        val node = ObjectMapper().readTree(
            """{"tekst":"æ","liste":[1,{"nøkkel":true}],"tom":[],"barn":{"tall":2},"tomtObjekt":{}}"""
        )

        val forventet = """
            {
                "tekst": "æ",
                "liste": [
                    1,
                    {
                        "nøkkel": true
                    }
                ],
                "tom": [],
                "barn": {
                    "tall": 2
                },
                "tomtObjekt": {}
            }
        """.trimIndent()

        assertEquals(forventet, formaterStatistikkFixtureJson(node))
    }
}
