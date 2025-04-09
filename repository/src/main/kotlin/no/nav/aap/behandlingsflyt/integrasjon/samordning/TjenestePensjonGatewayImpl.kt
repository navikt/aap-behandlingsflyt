package no.nav.aap.behandlingsflyt.integrasjon.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.TjenestePensjonGateway
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper.fromJson
import no.nav.aap.lookup.gateway.Factory
import java.net.URI

class TjenestePensjonGatewayImpl : TjenestePensjonGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.tjenestepensjon.url") + "/api/tjenestepensjon/tpNrWithYtelse")
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.tjenestepensjon.scope"))

    companion object : Factory<TjenestePensjonGateway> {
        override fun konstruer(): TjenestePensjonGateway {
            return TjenestePensjonGatewayImpl()
        }
    }

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    override fun hentTjenestePensjon(request: TjenestePensjonRequest): TjenestePensjon {
        val httpRequest = GetRequest(
            additionalHeaders = listOf(
                Header("fnr", request.fnr),
            )
        )
        println("full url: ${URI.create("${url}?fomDate=${request.periode.fom}&tomDate=${request.periode.tom}")}")
        return requireNotNull(client.get(
            uri = URI.create("${url}?fomDate=${request.periode.fom}&tomDate=${request.periode.tom}"),
            request = httpRequest,
            mapper = { body, _ ->
                val list = fromJson<List<String>>(body)
                TjenestePensjon(list)
            }
        ))
    }
}