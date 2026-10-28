package no.nav.aap.behandlingsflyt.arena

import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoMedVedtakResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdato
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ArenaMaksdatoMappingTest {
    @Test
    fun `bruker unntaksmaksdato foran ordinær maksdato`() {
        val response = response(
            """
                {
                  "sak": {
                    "sakId": 1,
                    "saknummer": "123-456",
                    "sakStatus": "AKTIV",
                    "sakRegistrert": "2024-01-01",
                    "sisteVedtak": {
                      "vedtakId": 1,
                      "aktfaseKode": "AKTIV",
                      "vedtaktypeKode": "O",
                      "vedtakstatuskode": "IVERK",
                      "maxdatoUnntak": "2025-06-01",
                      "maxdatoOrdinaer": "2025-01-01",
                      "til": "2024-12-31"
                    }
                  }
                }
            """.trimIndent()
        )

        assertThat(response.tilDomene()).isEqualTo(
            ArenaMaksdato(
                arenaSaksnummer = "123-456",
                status = "AKTIV",
                maksdato = LocalDate.of(2025, 6, 1),
                vedtakTil = LocalDate.of(2024, 12, 31),
            )
        )
    }

    @Test
    fun `bruker ordinær maksdato når unntaksmaksdato mangler`() {
        val response = response(
            """
                {
                  "sak": {
                    "sakId": 1,
                    "saknummer": "123-456",
                    "sakStatus": "AKTIV",
                    "sakRegistrert": "2024-01-01",
                    "sisteVedtak": {
                      "vedtakId": 1,
                      "aktfaseKode": "AKTIV",
                      "vedtaktypeKode": "O",
                      "vedtakstatuskode": "IVERK",
                      "maxdatoUnntak": null,
                      "maxdatoOrdinaer": "2025-01-01",
                      "til": "2024-12-31"
                    }
                  }
                }
            """.trimIndent()
        )

        assertThat(response.tilDomene()?.maksdato).isEqualTo(LocalDate.of(2025, 1, 1))
    }

    @Test
    fun `mangler sak gir null`() {
        assertThat(response("{\"sak\":null}").tilDomene()).isNull()
    }

    private fun response(json: String) = DefaultJsonMapper.fromJson<MaksdatoMedVedtakResponse>(json)
}
