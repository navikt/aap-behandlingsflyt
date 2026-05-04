package no.nav.aap.behandlingsflyt.integrasjon.pdfgen

import no.nav.aap.behandlingsflyt.behandling.meldekort.MeldekortPdfRequest
import no.nav.aap.behandlingsflyt.behandling.meldekort.PdfgenGateway
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import java.net.URI

class PdfgenGatewayImpl : PdfgenGateway {
    private val baseUri = URI.create(requiredConfigForKey("integrasjon.pdfgen.url"))
    private val client = RestClient.withDefaultResponseHandler(
        config = ClientConfig(),
        tokenProvider = NoTokenTokenProvider(),
        prometheus = prometheus
    )

    override fun genererMeldekortPdf(request: MeldekortPdfRequest): ByteArray {
        val uri = baseUri.resolve("/api/v1/genpdf/aap-saksbehandling-meldekort/meldekort")
        val pdf = requireNotNull(
            client.post(uri = uri, request = PostRequest(body = request), mapper = { body, _ -> body })
        )
        return pdf.readBytes()
    }

    companion object : Factory<PdfgenGateway> {
        override fun konstruer(): PdfgenGateway = PdfgenGatewayImpl()
    }
}