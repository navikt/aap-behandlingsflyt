package no.nav.aap.behandlingsflyt.integrasjon.datadeling

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.aap.behandlingsflyt.datadeling.sam.SamGateway
import no.nav.aap.behandlingsflyt.datadeling.sam.SamIdOgTpNr
import no.nav.aap.behandlingsflyt.datadeling.sam.SamordneVedtakRequest
import no.nav.aap.behandlingsflyt.datadeling.sam.SamordneVedtakRespons
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureM2MTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDate

class SamGatewayImpl : SamGateway {
    companion object : Factory<SamGateway> {
        override fun konstruer(): SamGateway {
            return SamGatewayImpl()
        }
    }

    private val logger = LoggerFactory.getLogger(SamGatewayImpl::class.java)

    private val restClient = RestClient.withDefaultResponseHandler(
        config = ClientConfig(scope = requiredConfigForKey("INTEGRASJON_SAM_SCOPE")),
        tokenProvider = AzureM2MTokenProvider,
        prometheus = prometheus
    )

    private val uri = URI.create(requiredConfigForKey("INTEGRASJON_SAM_URL"))

    override fun varsleVedtak(request: SamordneVedtakRequest) {
        restClient.post<SamordneVedtakRequest, SamordneVedtakRespons>(
            uri = uri.resolve("/api/vedtak/samordne"),
            request = PostRequest(body = request),
            mapper = { body, _ ->
                DefaultJsonMapper.fromJson(body)
            }
        )
    }

    override fun hentSamId(ident: Ident, sakId: Long, vedtakId: Long): List<SamIdOgTpNr> {
        return requireNotNull(
            restClient.get(
                uri = uri.resolve("/api/vedtak?sakId=$sakId&vedtakId=$vedtakId&fagomrade=AAP"),
                request = GetRequest(
                    additionalHeaders = listOf(Header("pid", ident.identifikator))
                ),
                mapper = { body, _ ->
                    val respons = DefaultJsonMapper.fromJson<List<SamordningsvedtakApi>>(body)
                    logger.info("respons fra SAM ved henting av samId: $respons")
                    respons.flatMap { samordningsvedtakApi ->
                        samordningsvedtakApi.samordningsmeldinger.map {
                            SamIdOgTpNr(
                                it.samId,
                                it.tpNr.toLong()
                            )
                        }
                    }
                }
            )) { "Fikk ikke respons for sakId $sakId." }
    }

    // https://github.com/navikt/sam/blob/541363d19bb6f0596562159b49c676d09631138e/provider/nav-provider-stotte-sam-app/src/main/java/no/nav/provider/stotte/sam/app/samordnevedtak/SamordneVedtakController.kt#L13
    private data class SamordningsvedtakApi(
        val samordningsmeldinger: List<SamordningsmeldingApi> = emptyList()
    )

    /**
     * https://github.com/navikt/sam/blob/master/provider/nav-provider-stotte-sam-app/src/main/java/no/nav/provider/stotte/sam/app/samordnevedtak/Samordning.kt
     */
    private data class SamordningsmeldingApi(
        val samId: Long,
        val meldingstatusKode: String,
        val tpNr: String,
        val tpNavn: String,
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Europe/Oslo")
        val sendtDato: LocalDate,
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Europe/Oslo")
        val svartDato: LocalDate?,
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Europe/Oslo")
        val purretDato: LocalDate?,
        val refusjonskrav: Boolean,
        val versjon: Long
    )
}
