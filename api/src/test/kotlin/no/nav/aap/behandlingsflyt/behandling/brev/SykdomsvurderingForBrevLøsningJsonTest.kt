package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SykdomsvurderingForBrevLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.Dokumentkilde
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.json.DeserializationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class SykdomsvurderingForBrevLøsningJsonTest {
    @Test
    fun `deserialiserer gammel løsning uten kilder`() {
        val løsning = deserialiserLøsning("""{"behovstype":"5053","vurdering":"Vurdering uten kilder"}""")

        assertThat(løsning.behovstype).isEqualTo(AvklaringsbehovKode.`5053`)
        assertThat(løsning.vurdering).isEqualTo("Vurdering uten kilder")
        assertThat(løsning.kilder).isNull()
    }

    @Test
    fun `deserialiserer eksplisitt null og tom kildeliste som ulike løsninger`() {
        val utenEndring = deserialiserLøsning("""{"behovstype":"5053","vurdering":"Vurdering","kilder":null}""")
        val tømKilder = deserialiserLøsning("""{"behovstype":"5053","vurdering":"Vurdering","kilder":[]}""")

        assertThat(utenEndring.kilder).isNull()
        assertThat(tømKilder.kilder).isNotNull().isEmpty()
    }

    @Test
    fun `deserialiserer løsning med dokumentkilde og journalføringsdato`() {
        val løsning = deserialiserLøsning(
            """
                {
                  "behovstype": "5053",
                  "vurdering": "Vurdering med kilder",
                  "kilder": [{
                    "journalpostId": "123456789",
                    "dokumentInfoId": "987654321",
                    "dokumentnavn": "Legeerklæring",
                    "journalføringsdato": "2024-01-15"
                  }]
                }
            """.trimIndent()
        )

        assertThat(løsning.vurdering).isEqualTo("Vurdering med kilder")
        assertThat(løsning.kilder).containsExactly(
            Dokumentkilde("123456789", "987654321", "Legeerklæring", LocalDate.of(2024, 1, 15))
        )
    }

    @Test
    fun `avviser dokumentkilde uten journalføringsdato`() {
        val exception = assertThrows<DeserializationException> {
            deserialiserLøsning(
                """
                    {
                      "behovstype": "5053",
                      "vurdering": "Vurdering",
                      "kilder": [{
                        "journalpostId": "123456789",
                        "dokumentInfoId": "987654321",
                        "dokumentnavn": "Legeerklæring"
                      }]
                    }
                """.trimIndent()
            )
        }

        assertThat(exception.message).contains("journalføringsdato")
    }

    @Test
    fun `avviser dokumentkilde med null eller ugyldig journalføringsdato`() {
        listOf("null", "\"ikke-en-dato\"", "\"2024-13-15\"", "\"2024-02-30\"").forEach { dato ->
            val exception = assertThrows<DeserializationException> {
                deserialiserLøsning(
                    """
                        {
                          "behovstype": "5053",
                          "vurdering": "Vurdering",
                          "kilder": [{
                            "journalpostId": "123456789",
                            "dokumentInfoId": "987654321",
                            "dokumentnavn": "Legeerklæring",
                            "journalføringsdato": $dato
                          }]
                        }
                    """.trimIndent()
                )
            }

            assertThat(exception.message).contains("journalføringsdato")
        }
    }

    private fun deserialiserLøsning(json: String): SykdomsvurderingForBrevLøsning =
        DefaultJsonMapper.fromJson(json, SykdomsvurderingForBrevLøsning::class.java)
}
