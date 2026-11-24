package no.nav.aap.behandlingsflyt.arena

import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoSamordningResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaVedtakStatus
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ArenaMaksdatoMappingTest {
    @Test
    fun `mapper løpende vedtak med maksdato`() {
        val response = response(
            """
                {
                  "sisteAktuelleVedtak": {
                    "vedtakId": 1,
                    "saknummer": "123-456",
                    "maksdato": "2025-06-01",
                    "status": "LØPENDE",
                    "harForlengelseEtter11_12": false,
                    "vedtakTil": "2024-12-31"
                  }
                }
            """.trimIndent()
        )

        assertThat(response.tilDomene()).isEqualTo(
            ArenaMaksdato(
                arenaSaksnummer = "123-456",
                vedtakStatus = ArenaVedtakStatus.LØPENDE,
                maksdato = LocalDate.of(2025, 6, 1),
                vedtakTil = LocalDate.of(2024, 12, 31),
                vedtakId = 1,
                harForlengelseEtter11_12 = false,
            )
        )
    }

    @Test
    fun `stanset vedtak gir ingen maksdato`() {
        val response = response(
            """
                {
                  "sisteAktuelleVedtak": {
                    "vedtakId": 1,
                    "saknummer": "123-456",
                    "maksdato": null,
                    "status": "STANSET",
                    "harForlengelseEtter11_12": false
                  }
                }
            """.trimIndent()
        )

        val arenaMaksdato = response.tilDomene()
        assertThat(arenaMaksdato?.vedtakStatus).isEqualTo(ArenaVedtakStatus.STANSET)
        assertThat(arenaMaksdato?.maksdato).isNull()
    }

    @Test
    fun `mapper forlengelse etter 11-12 og manglende vedtakTil gir null`() {
        val response = response(
            """
                {
                  "sisteAktuelleVedtak": {
                    "vedtakId": 1,
                    "saknummer": "123-456",
                    "maksdato": "2025-06-01",
                    "status": "AVSLUTTET",
                    "harForlengelseEtter11_12": true
                  }
                }
            """.trimIndent()
        )

        val arenaMaksdato = response.tilDomene()
        assertThat(arenaMaksdato?.vedtakStatus).isEqualTo(ArenaVedtakStatus.AVSLUTTET)
        assertThat(arenaMaksdato?.harForlengelseEtter11_12).isTrue()
        assertThat(arenaMaksdato?.vedtakTil).isNull()
    }

    @Test
    fun `mangler vedtak gir null`() {
        assertThat(response("{\"sisteAktuelleVedtak\":null}").tilDomene()).isNull()
        assertThat(MaksdatoSamordningResponse.INGEN.tilDomene()).isNull()
    }

    private fun response(json: String) = DefaultJsonMapper.fromJson<MaksdatoSamordningResponse>(json)
}
