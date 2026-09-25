package no.nav.aap.behandlingsflyt.integrasjon.arena

import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import no.nav.aap.behandlingsflyt.arena.ArenaOppslagGateway
import no.nav.aap.behandlingsflyt.arena.ArenaSakerRequest
import no.nav.aap.behandlingsflyt.arena.ArenaSakerResponse
import no.nav.aap.behandlingsflyt.arena.ArenaSykdomsvurderingResponse
import no.nav.aap.behandlingsflyt.arena.HarArenaHistorikkRequest
import no.nav.aap.behandlingsflyt.arena.HarArenaHistorikkResponse
import no.nav.aap.behandlingsflyt.arena.KravFraArenaResponse
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
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
            .build<String, HarArenaHistorikkResponse>()

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

    override fun hentHarHistorikk(ident: Ident): HarArenaHistorikkResponse {
       return harArenaHistorikkCache.get(ident.identifikator) {
            val response: HarArenaHistorikkResponse? = restClient.post(
                uri.resolve("/api/v1/person/historikk"),
                PostRequest(
                    body = HarArenaHistorikkRequest(ident.identifikator),
                    timeout = Duration.ofSeconds(5)
                ),
                mapper = { body, _ -> DefaultJsonMapper.fromJson(body) }
            )
            requireNotNull(response) { "Fikk ikke gyldig svar fra /api/v1/person/historikk" }
            response
        }
    }

    override fun hentSakerForPerson(ident: Ident): ArenaSakerResponse {
        val response: ArenaSakerResponse? = restClient.post(
            uri.resolve("/api/v1/person/saker"),
            PostRequest(
                body = ArenaSakerRequest(ident.identifikator),
                timeout = Duration.ofSeconds(5)
            ),
            mapper = { body, _ -> DefaultJsonMapper.fromJson<ArenaSakerResponse>(body) }
        )
        requireNotNull(response) { "Fikk ikke gyldig svar fra /api/v1/person/saker" }
        return response
    }

    override fun hentKravDataForSak(arenasaksnummer: String): KravFraArenaResponse {
        val response: KravFraArenaResponse? = restClient.get(
            uri.resolve("/api/migrering/$arenasaksnummer/krav"),
            GetRequest(timeout = Duration.ofSeconds(5)),
            mapper = { body, _ -> DefaultJsonMapper.fromJson<KravFraArenaResponse>(body) }
        )
        requireNotNull(response) { "Fikk ikke gyldig svar fra /api/migrering/$arenasaksnummer/krav" }
        return response
    }

    override fun hentSykdomsvurdering(saksnummerArena: String): ArenaSykdomsvurderingResponse {
        val response: ArenaSykdomsvurderingResponse? = restClient.get(
            uri.resolve("/api/migrering/${saksnummerArena}/sykdom"),
            GetRequest(timeout = Duration.ofSeconds(5)),
            mapper = { body, _ -> DefaultJsonMapper.fromJson(body) }
        )
        requireNotNull(response) { "Fikk ikke gyldig svar fra /api/migrering/${saksnummerArena}/sykdom" }
        return response
    }
}
