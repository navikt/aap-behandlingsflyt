package no.nav.aap.behandlingsflyt.integrasjon.datadeling

import no.nav.aap.behandlingsflyt.datadeling.sam.HentSamIdResponse
import no.nav.aap.behandlingsflyt.datadeling.sam.SamGateway
import no.nav.aap.behandlingsflyt.datadeling.sam.SamordneVedtakRequest
import no.nav.aap.behandlingsflyt.datadeling.sam.SamordneVedtakRespons
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.samid.SamId
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.net.URI
import java.time.LocalDate

class SamGatewayImpl : SamGateway {
    companion object : Factory<SamGateway> {
        override fun konstruer(): SamGateway {
            return SamGatewayImpl()
        }
    }

    private val restClient = RestClient.withDefaultResponseHandler(
        config = ClientConfig(scope = requiredConfigForKey("integrasjon.sam.scope")),
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    val uri = URI.create(requiredConfigForKey("integrasjon.sam.url"))


    override fun varsleVedtak(request: SamordneVedtakRequest) {
        restClient.post<SamordneVedtakRequest, SamordneVedtakRespons>(
            uri = uri.resolve("/api/vedtak/samordne"),
            request = PostRequest(body = request),
            mapper = { body, _ ->
                DefaultJsonMapper.fromJson(body)
            }
        )
    }

    override fun hentSamId(ident: Ident, sakId: Long, vedtakId: Long): Long { //returnerer List<SamordningsvedtakApi> ????????
        return requireNotNull(restClient.get(
            uri = uri.resolve("/api/vedtak?sakId=$sakId&vedtakId=$vedtakId&fagomrade=AAP"),
            request = GetRequest(
                additionalHeaders = listOf(Header("pid", ident.identifikator))
            ),
            mapper = { body, _ ->
                val respons = DefaultJsonMapper.fromJson<List<SamordningsvedtakApi>>(body).first()
                require(respons.samordningsmeldinger.size<2 && respons.samordningsmeldinger.isNotEmpty()) {"for mangen eller for få samordningsmeldinger i responsen fra SAM ved henting av samId. Antall samordningsmeldinger: ${respons.samordningsmeldinger.size}"}
                val res = respons.samordningsmeldinger.first().samId
                return@get res
            }
        ))
    }

    // https://github.com/navikt/sam/blob/656cc706c16bcdba7ce626525f8777bc2a2e35d3/provider/nav-provider-stotte-sam-app/src/main/java/no/nav/provider/stotte/sam/app/SamordneVedtakController.kt
    private data class SamordningsvedtakApi(
        val samordningsmeldinger: List<SamordningsmeldingApi> = emptyList()
    )

    private data class SamordningsmeldingApi(
        val samId: Long,
    )
}
