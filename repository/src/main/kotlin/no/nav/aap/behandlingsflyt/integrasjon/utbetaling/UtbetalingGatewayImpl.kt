package no.nav.aap.behandlingsflyt.integrasjon.utbetaling

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.HåndterConflictResponseHandler
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingGateway
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDto
import java.net.URI
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.utbetal.simulering.UtbetalingOgSimuleringDto


object UtbetalingGatewayImpl: UtbetalingGateway {

    private val baseUri = URI.create(requiredConfigForKey("integrasjon.utbetal.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.utbetal.scope"))

    private val client = RestClient(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        responseHandler = HåndterConflictResponseHandler(),
        prometheus = prometheus
    )

    override fun utbetal(tilkjentYtelseDto: TilkjentYtelseDto) {
        val url = baseUri.resolve("/tilkjentytelse")
        val request = PostRequest<TilkjentYtelseDto>(
            body = tilkjentYtelseDto
        )
        client.post<TilkjentYtelseDto, Unit?>(
            uri = url,
            request = request,
            mapper = { _, _ -> }
        )
    }

    override fun simulering(tilkjentYtelseDto: TilkjentYtelseDto): List<UtbetalingOgSimuleringDto> {
        val url = baseUri.resolve("/simulering")
        val request = PostRequest<TilkjentYtelseDto>(
            body = tilkjentYtelseDto
        )
        return client.post(
            uri = url,
            request = request
        )!!
    }
}