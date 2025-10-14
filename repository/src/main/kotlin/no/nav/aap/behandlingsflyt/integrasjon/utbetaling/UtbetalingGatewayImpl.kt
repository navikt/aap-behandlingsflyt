package no.nav.aap.behandlingsflyt.integrasjon.utbetaling

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.HåndterConflictResponseHandler
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingGateway
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDto
import java.net.URI
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.utbetal.simulering.UtbetalingOgSimuleringDto
import no.nav.aap.utbetal.trekk.TrekkResponsDto


object UtbetalingGatewayImpl : UtbetalingGateway {

    private val baseUri = URI.create(requiredConfigForKey("integrasjon.utbetal.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.utbetal.scope"))

    private val client = RestClient(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        responseHandler = HåndterConflictResponseHandler(),
        prometheus = prometheus
    )

    override fun utbetal(tilkjentYtelseDto: TilkjentYtelseDto) {
        client.post<TilkjentYtelseDto, Unit?>(
            uri = baseUri.resolve("/tilkjentytelse"),
            request = PostRequest(body = tilkjentYtelseDto),
            mapper = { _, _ -> }
        )
    }

    override fun simulering(tilkjentYtelseDto: TilkjentYtelseDto): List<UtbetalingOgSimuleringDto> {
        return client.post(
            uri = baseUri.resolve("/simulering"),
            request = PostRequest(body = tilkjentYtelseDto)
        )!!
    }

    override fun hentTrekk(saksnummer: String): TrekkResponsDto {
        val request = GetRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        val url = baseUri.resolve("/trekk/$saksnummer")
        val response: TrekkResponsDto = requireNotNull(
            client.get(
                uri = url, request = request, mapper = { body, _ ->
                    DefaultJsonMapper.fromJson(body)
                })
        )

        return response
    }
}