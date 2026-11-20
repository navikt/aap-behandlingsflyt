package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate.LøsAvklaringsbehovPåBehandling
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate.LøsPeriodisertAvklaringsbehovPåBehandling
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarVedtakslengdeLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarVedtakslengdePeriodisertLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_VEDTAKSLENGDE_KODE
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AvklaringsbehovApiKompatibilitetTest {
    private val mapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    private val requestBody = """
        {
          "referanse": "00000000-0000-0000-0000-000000000001",
          "behandlingVersjon": 1,
          "behov": {
            "behovstype": "$AVKLAR_VEDTAKSLENGDE_KODE",
            "løsningerForPerioder": [{
              "fom": "2025-02-01",
              "tom": "2025-02-02",
              "begrunnelse": "Begrunnelse",
              "sluttdato": "2025-12-31",
              "årsaker": []
            }]
          }
        }
    """.trimIndent()

    @Test
    fun `vedtakslengde-payload kan leses av begge requesttypene`() {
        val vanligRequest = mapper.readValue(requestBody, LøsAvklaringsbehovPåBehandling::class.java)
        val periodisertRequest = mapper.readValue(requestBody, LøsPeriodisertAvklaringsbehovPåBehandling::class.java)

        assertTrue(vanligRequest.behov is AvklarVedtakslengdeLøsning)
        assertTrue(periodisertRequest.behov is AvklarVedtakslengdePeriodisertLøsning)
    }
}
