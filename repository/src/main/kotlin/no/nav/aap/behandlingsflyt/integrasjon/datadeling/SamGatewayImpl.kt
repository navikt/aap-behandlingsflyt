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
        config = ClientConfig(scope = requiredConfigForKey("sam.scope")),
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    val uri = URI.create(requiredConfigForKey("integrasjon.sam.url"))


    override fun varsleVedtak(request: SamordneVedtakRequest) {
        restClient.post<SamordneVedtakRequest, SamordneVedtakRespons>(
            uri = uri.resolve("/api/vedtak"),
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
                DefaultJsonMapper.fromJson<List<SamordningsvedtakApi>>(body).first().samordningsmeldinger.first().samId //todo: kast feil dersom vi får flere
            }
        ))
    }


    data class SamordningsvedtakApi(
        val samordningVedtakId: Long,
        val fagsystem: String,
        val saksId: Long,
        val saksKode: String,
        val vedtakId: Long,
        val vedtakstatusKode: String?,
        val etterbetaling: Boolean,
        val utvidetSamordningsfrist: Boolean,
        val virkningFom: LocalDate,
        val virkningTom: LocalDate?,
        val versjon: Long,
        val samordningsmeldinger: List<SamordningsmeldingApi> = emptyList()
    )

    data class SamordningsmeldingApi(
        val samId: Long,
        val meldingstatusKode: String,
        val tpNr: String,
        val tpNavn: String,
        val sendtDato: LocalDate,
        val svartDato: LocalDate?,
        val purretDato: LocalDate?,
        val refusjonskrav: Boolean,
        val versjon: Long
    )
}
