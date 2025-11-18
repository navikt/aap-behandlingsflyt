package no.nav.aap.behandlingsflyt.integrasjon.organisasjon

import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfo
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoGateway
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattVisningsnavn
import no.nav.aap.behandlingsflyt.integrasjon.util.GraphQLResponse
import no.nav.aap.behandlingsflyt.integrasjon.util.GraphQLResponseHandler
import no.nav.aap.behandlingsflyt.integrasjon.util.GraphqlRequest
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import java.net.URI
import java.time.LocalDate
import java.time.Duration


class NomInfoGateway : AnsattInfoGateway {

    private val graphqlUrl = URI.create(requiredConfigForKey("integrasjon.nom.url"))
    private val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.nom.scope"),
    )

    private val client = RestClient(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        responseHandler = GraphQLResponseHandler(),
        prometheus = prometheus
    )

    override fun hentAnsattInfo(navIdent: String): AnsattInfo = ansattInfoCache.get(navIdent) {
        val request = GraphqlRequest(ressursQuery, NomRessursVariables(navIdent))
        val response = checkNotNull(query(request).data) {
            "Fant ikke ansatt ($navIdent) i NOM"
        }

        mapResponse(navIdent, checkNotNull(response.ressurs))
    }

    override fun hentAnsatteVisningsnavn(navIdenter: List<String>): List<AnsattVisningsnavn?> = ansattVisningsnavnCache.get(navIdenter) {
        val request = GraphqlRequest(flereNavnQuery, NomRessurserVariables(navIdenter))
        val response = checkNotNull(flereNavnQuery(request).data) {
            "Fant ikke ansatt i NOM"
        }

        response.ressurser.map {
            if(it.ressurs != null) AnsattVisningsnavn(it.ressurs.navident, it.ressurs.visningsnavn) else null
        }
    }

    private fun flereNavnQuery(request: GraphqlRequest<NomRessurserVariables>): GraphQLResponse<NomRessurserVisningsnavn> {
        val httpRequest = PostRequest(body = request)
        return requireNotNull(client.post(uri = graphqlUrl, request = httpRequest))
    }

    private fun query(request: GraphqlRequest<NomRessursVariables>): GraphQLResponse<NomData> {
        val httpRequest = PostRequest(body = request)
        return requireNotNull(client.post(uri = graphqlUrl, request = httpRequest))
    }

    private fun mapResponse(navIdent: String, nomDataRessurs: NomDataRessurs): AnsattInfo {
        return AnsattInfo(
            navIdent = navIdent,
            navn = nomDataRessurs.visningsnavn,
            enhetsnummer = finnAnsattEnhetsnummer(nomDataRessurs)
        )
    }

    private fun finnAnsattEnhetsnummer(nomDataRessurs: NomDataRessurs): String {
        val enhet = nomDataRessurs.orgTilknytning.single {
            it.erAktiv() && it.erDagligOppfolging
        }.orgEnhet

        return checkNotNull(enhet.remedyEnhetId)
    }

    private fun OrgTilknytning.erAktiv(): Boolean {
        val iDag = LocalDate.now()
        return gyldigFom <= iDag && (gyldigTom == null || gyldigTom >= iDag)
    }

    companion object : Factory<AnsattInfoGateway> {
        override fun konstruer(): AnsattInfoGateway = NomInfoGateway()

        private val ansattInfoCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofHours(1))
            .recordStats()
            .build<String, AnsattInfo>()

        private val ansattVisningsnavnCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofHours(6))
            .recordStats()
            .build<List<String>, List<AnsattVisningsnavn?>>()

        init {
            CaffeineCacheMetrics.monitor(prometheus, ansattInfoCache, "nom_ansatt_info")
            CaffeineCacheMetrics.monitor(prometheus, ansattVisningsnavnCache, "nom_ansatte_visningsnavn")
        }
    }
}

private const val navIdent = $$"$navIdent"
val ressursQuery = """
    query($navIdent: String!) {
      ressurs(where: {navident: $navIdent}) {
        orgTilknytning {
          orgEnhet {
            remedyEnhetId
          }
          erDagligOppfolging
          gyldigFom
          gyldigTom
        }
        visningsnavn
      }
    }
""".trimIndent()

private const val navIdenter = $$"$navIdenter"
val flereNavnQuery = """
    query($navIdenter: [String!]) {
        ressurser(where: {navidenter: $navIdenter}) {
            ressurs {
                navident
                visningsnavn
            }
        }
    }
""".trimIndent()
