package no.nav.aap.behandlingsflyt.integrasjon.arena

import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import no.nav.aap.behandlingsflyt.arena.ArenaOppslagGateway
import no.nav.aap.behandlingsflyt.arena.HarHistorikkRequest
import no.nav.aap.behandlingsflyt.arena.HarHistorikkResponse
import no.nav.aap.behandlingsflyt.arena.SakerRequest
import no.nav.aap.behandlingsflyt.arena.SakerResponse
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureM2MTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.net.URI
import java.time.Duration

class ArenaOppslagGatewayImpl : ArenaOppslagGateway {

    companion object : Factory<ArenaOppslagGateway> {
        override fun konstruer(): ArenaOppslagGateway {
            return ArenaOppslagGatewayImpl()
        }

        private val harArenaHistorikkCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(2))
            .maximumSize(10_000)
            .recordStats()
            .build<String, HarHistorikkResponse>()

        init {
            CaffeineCacheMetrics.monitor(prometheus, harArenaHistorikkCache, "arenaoppslag_historikk")
        }
    }

    private val uri = URI.create(requiredConfigForKey("INTEGRASJON_ARENAOPPSLAG_URL"))
    private val config = ClientConfig(scope = requiredConfigForKey("INTEGRASJON_ARENAOPPSLAG_SCOPE"))
    private val restClient = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = AzureM2MTokenProvider,
        prometheus = prometheus
    )

    override fun hentHarHistorikk(personidentifikator: String): HarHistorikkResponse {
       return harArenaHistorikkCache.get(personidentifikator) {
            val response: HarHistorikkResponse? = restClient.post(
                uri.resolve("/api/v1/person/historikk"),
                PostRequest(body = HarHistorikkRequest(personidentifikator)),
                mapper = { body, _ -> DefaultJsonMapper.fromJson(body) }
            )
            requireNotNull(response) { "Fikk ikke gyldig svar fra /api/v1/person/historikk" }
            response
        }
    }

    override fun hentSakerForPerson(personidentifikator: String): SakerResponse {
        val response: SakerResponse? = restClient.post(
            uri.resolve("/api/v1/person/saker"),
            PostRequest(body = SakerRequest(personidentifikator)),
            mapper = { body, _ -> DefaultJsonMapper.fromJson<SakerResponse>(body) }
        )
        requireNotNull(response) { "Fikk ikke gyldig svar fra /api/v1/person/saker" }
        return response
    }
}
