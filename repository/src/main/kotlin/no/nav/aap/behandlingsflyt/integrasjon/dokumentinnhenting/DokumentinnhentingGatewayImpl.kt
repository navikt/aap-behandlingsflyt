package no.nav.aap.behandlingsflyt.integrasjon.dokumentinnhenting

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.DokumentinnhentingGateway
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.dokumentinnhenting.kontrakt.BehandlingsflytToDokumentInnhentingBestillingDto
import no.nav.aap.dokumentinnhenting.kontrakt.DialogmeldingForhåndsvisningDto
import no.nav.aap.dokumentinnhenting.kontrakt.DialogmeldingStatusTilBehandslingsflytDto
import no.nav.aap.dokumentinnhenting.kontrakt.ForhåndsvisDialogmeldingDto
import no.nav.aap.dokumentinnhenting.kontrakt.LegeerklæringPurringDto
import no.nav.aap.dokumentinnhenting.kontrakt.MarkerBestillingSomMottattDto
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureM2MTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.net.URI

/**
 * Bestiller dokumenter fra dokumentinnhenting
 */
class DokumentinnhentingGatewayImpl : DokumentinnhentingGateway {
    private val syfoUri = requiredConfigForKey("INTEGRASJON_DOKUMENTINNHENTING_URL") + "/syfo"
    private val config = ClientConfig(scope = requiredConfigForKey("INTEGRASJON_DOKUMENTINNHENTING_SCOPE"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = AzureM2MTokenProvider,
        prometheus = prometheus
    )

    companion object : Factory<DokumentinnhentingGateway> {
        override fun konstruer(): DokumentinnhentingGateway {
            return DokumentinnhentingGatewayImpl()
        }
    }

    override fun bestillLegeerklæring(request: BehandlingsflytToDokumentInnhentingBestillingDto): String {
        val request = PostRequest(
            body = request,
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )

        return requireNotNull(client.post(uri = URI.create("$syfoUri/dialogmeldingbestilling"), request))
    }

    override fun purrPåLegeerklæring(purringRequest: LegeerklæringPurringDto): String {
        val request = PostRequest(
            body = purringRequest,
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )
        return requireNotNull(client.post(uri = URI.create("$syfoUri/purring"), request))
    }

    override fun markerDialogmeldingStatusSomMottatt(markerSomMottattRequest: MarkerBestillingSomMottattDto): DialogmeldingStatusTilBehandslingsflytDto {
        val request = PostRequest(
            body = markerSomMottattRequest,
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )

        return requireNotNull(
            client.post(
                uri = URI.create("$syfoUri/status/markerbestillingmottatt"),
                request = request,
                mapper = { body, _ -> DefaultJsonMapper.fromJson(body) }
            )
        )
    }

    override fun legeerklæringStatus(saksnummer: String): List<DialogmeldingStatusTilBehandslingsflytDto> {
        val request = GetRequest(
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )

        return requireNotNull(
            client.get(
                uri = URI.create("$syfoUri/status/$saksnummer"),
                request = request,
                mapper = { body, _ -> DefaultJsonMapper.fromJson(body) })
        )
    }

    override fun forhåndsvisDialogmelding(request: ForhåndsvisDialogmeldingDto): DialogmeldingForhåndsvisningDto {
        val request = PostRequest(
            body = request,
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )

        return requireNotNull(client.post(uri = URI.create("$syfoUri/brevpreview"), request))
    }
}