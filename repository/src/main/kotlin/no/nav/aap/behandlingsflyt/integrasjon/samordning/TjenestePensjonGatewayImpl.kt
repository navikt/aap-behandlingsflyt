package no.nav.aap.behandlingsflyt.integrasjon.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.SamhandlerForholdDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonRespons
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.TjenestePensjonGateway
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper.fromJson
import no.nav.aap.komponenter.type.Periode
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

    fun hentFullTjenestePensjon(ident: String){
        val httpRequest = GetRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json"),
                Header("fnr", ident),
            )
        )

        client.get(
            uri = url,
            request = httpRequest,
            mapper = { body, _ ->
                fromJson<TjenestePensjon>(body)
            }
        )
    }

    override fun hentTjenestePensjon(ident: String, periode: Periode): List<SamhandlerForholdDto> {
        val httpRequest = GetRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json"),
                Header("fnr", ident),
            )
        )

        return requireNotNull(client.get(
            uri = URI.create("${url}?fomDate=${periode.fom}&tomDate=${periode.tom}"),
            request = httpRequest,
            mapper = { body, _ ->
                fromJson<TjenestePensjonRespons>(body)
            }
        )).forhold
    }
}