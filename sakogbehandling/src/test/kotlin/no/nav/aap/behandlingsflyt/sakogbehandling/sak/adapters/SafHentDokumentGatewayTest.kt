package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.net.URI
import java.net.http.HttpHeaders

class SafHentDokumentGatewayTest {
    @Test
    fun `test at mocket respons returneres`() {
        // TODO: noe refaktorering i Restclient for å gjøre denne testen mer meningsfull
        System.setProperty("integrasjon.saf.scope", "saf")
        System.setProperty("integrasjon.saf.url.rest", "http://localhost:1234/rest")

        val restClient = mockk<RestClient<InputStream>>()
        val gateway = SafHentDokumentGateway(restClient)

        val response = SafDocumentResponse(
            dokument = InputStream.nullInputStream(), contentType = "application/pdf", filnavn = "xxx.pdf"
        )

        val calls = slot<URI>()

        every {
            restClient.get(
                capture<URI>(calls), any(), any<(InputStream, HttpHeaders) -> SafDocumentResponse>()
            )
        } returns response

        val resp = gateway.hentDokument(JournalpostId("1234"), DokumentInfoId("123"), currentToken = mockk<OidcToken>())

        assertThat(calls.captured.toString()).isEqualTo("http://localhost:1234/rest/hentdokument/1234/123/ARKIV")
        assertThat(resp).isEqualTo(response)
    }
}