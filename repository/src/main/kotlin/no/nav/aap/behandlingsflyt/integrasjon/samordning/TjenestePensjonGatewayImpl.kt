package no.nav.aap.behandlingsflyt.integrasjon.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonForhold
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.TjenestePensjonGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.TjenestePensjonRespons
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper.fromJson
import no.nav.aap.komponenter.type.Periode
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * Se https://github.com/navikt/tp/blob/e99c670da41c23172e2ccc3a3e8dff4c7870fa82/tp-api/src/main/kotlin/no/nav/samhandling/tp/controller/TjenestepensjonController.kt#L117
 */
class TjenestePensjonGatewayImpl : TjenestePensjonGateway {
    private val url =
        URI.create(requiredConfigForKey("integrasjon.tjenestepensjon.url") + "/api/tjenestepensjon/getActiveForholdMedActiveYtelser")
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.tjenestepensjon.scope"))

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<TjenestePensjonGateway> {
        override fun konstruer(): TjenestePensjonGateway {
            return TjenestePensjonGatewayImpl()
        }
    }

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    override fun hentTjenestePensjon(ident: String, periode: Periode): List<TjenestePensjonForhold> {
        val httpRequest = GetRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json"),
                Header("fnr", ident),
            )
        )

        return try {
            requireNotNull(
                client.get(
                uri = URI.create("${url}?fomDate=${periode.fom}&tomDate=${periode.tom}"),
                request = httpRequest,
                mapper = { body, _ ->
                    fromJson<TjenestePensjonRespons?>(body)?.toIntern()
                }
            ))
        } catch (e: IkkeFunnetException) {
            log.info("Fikk 404 fra tp-registeret. Melding: ${e.message}")
            emptyList()
        }
    }
}