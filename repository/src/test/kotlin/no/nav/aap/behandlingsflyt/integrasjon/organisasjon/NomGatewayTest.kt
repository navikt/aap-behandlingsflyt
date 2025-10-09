package no.nav.aap.behandlingsflyt.integrasjon.organisasjon

import no.nav.aap.behandlingsflyt.test.Fakes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@Fakes
class NomGatewayTest {
    @Test
    fun kanHenteAnsatteVisningsnavn() {
        val nomGateway = NomInfoGateway()
        val visningsnavn = nomGateway.hentAnsatteVisningsnavn(listOf("ABC123", "DEF456"))
        assertEquals("Sak Behandlersen", visningsnavn.first()?.visningsnavn)
    }
    @Test
    fun kanHenteAnsattInfo() {
        val nomGateway = NomInfoGateway()
        val ansattinfo = nomGateway.hentAnsattInfo("ABC123")
        assertEquals("Test Testesen", ansattinfo.navn)
        assertEquals("1234", ansattinfo.enhetsnummer)
    }
}