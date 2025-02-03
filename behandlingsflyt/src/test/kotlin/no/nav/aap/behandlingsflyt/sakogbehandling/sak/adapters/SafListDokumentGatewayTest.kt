package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.SafHentDokumentGateway.Companion.extractFileNameFromHeaders
import no.nav.aap.behandlingsflyt.test.AzureTokenGen
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.http.HttpHeaders

@Fakes
class SafListDokumentGatewayTest {
    @Test
    fun `hente dokumentoversikt fra saf`() {
        val gateway = SafListDokumentGateway

        val tokenString = AzureTokenGen(
            issuer = "xdf",
            audience = "xxx"
        ).generate(true)
        // TODO: Generer fake token
        val token = OidcToken(tokenString)

        val documents = gateway.hentDokumenterForSak(Saksnummer("123"), token)
        assertThat(documents).hasSize(3)
    }

    @Test
    fun `hente ut filnavn fra header`() {
        val headers =
            HttpHeaders.of(mapOf("Content-Disposition" to listOf("inline; filename=400000000_ARKIV.pdf"))) { _, _ -> true }

        val response = extractFileNameFromHeaders(headers)

        assertThat(response).isEqualTo("400000000_ARKIV.pdf")
    }
}