package no.nav.aap.behandlingsflyt.integrasjon.pdfgenerator

import no.nav.aap.behandlingsflyt.behandling.vilkår.innsikt.Dokument
import no.nav.aap.behandlingsflyt.behandling.vilkår.innsikt.PdfGeneratorGateway
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import java.net.URI

class PdfGeneratorGatewayImpl : PdfGeneratorGateway {
    private val baseUri = URI.create(requiredConfigForKey("INTEGRASJON_PDFGENERATOR_URL"))
    private val config = ClientConfig(scope = requiredConfigForKey("INTEGRASJON_PDFGENERATOR_SCOPE"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = NoTokenTokenProvider(),
        prometheus = prometheus,
    )

    override fun genererVurderingerOppsummeringDokument(request: Dokument): ByteArray {
        val uri = baseUri.resolve("/api/v1/genpdf/innsikt/vurderinger")
        val httpRequest = PostRequest(
            body = request,
            additionalHeaders = listOf(
                Header("Accept", "application/pdf")
            )
        )

        return requireNotNull(
            client.post(uri, httpRequest) { body, _ -> body.readBytes() }
        )
    }

    companion object : Factory<PdfGeneratorGateway> {
        override fun konstruer(): PdfGeneratorGateway = PdfGeneratorGatewayImpl()
    }
}