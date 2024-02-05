package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.aap.HttpClientFactory
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.ktor.client.AzureAdTokenProvider
import no.nav.aap.ktor.client.AzureConfig
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PdlConfig(
    scope: String,
    url: String,
) : GraphQLConfig(
    scope = scope,
    url = url,
    additionalHeaders = listOf(
        "Nav-Consumer-Id" to "sakogbehandling",
        "TEMA" to "AAP",
    )
)

object PersonService {
    private lateinit var azureConfig: AzureConfig
    private lateinit var pdlConfig: PdlConfig
    private lateinit var graphQLClient: PdlClient

    fun init(
        azure: AzureConfig,
        pdl: PdlConfig
    ) {
        azureConfig = azure
        pdlConfig = pdl
        graphQLClient = PdlClient(azureConfig, pdlConfig)
    }

    // todo: caching av identer? slå opp i db før graphql?
    suspend fun hentPerson(ident: Ident, connection: DBConnection): Person {
        val identliste = hentAlleIdenterForPerson(ident)
        val personRepository = PersonRepository(connection)
        return personRepository.finnEllerOpprett(identliste)
    }

    // TODO: returner execption, option, result eller emptylist
    private suspend fun hentAlleIdenterForPerson(ident: Ident): List<Ident> {
        val request = PdlRequest(IDENT_QUERY, IdentVariables(ident.identifikator))
        val response: Result<PdlResponse> = graphQLClient.query(request)

        fun onSuccess(resp: PdlResponse): List<Ident> {
            return resp.data
                ?.hentIdenter
                ?.identer
                ?.filter { it.gruppe == PdlGruppe.FOLKEREGISTERIDENT }
                ?.map { Ident(identifikator = it.ident, aktivIdent = it.historisk.not()) }
                ?: emptyList()
        }

        fun onFailure(ex: Throwable): List<Ident> {
            SECURE_LOGGER.error("Feil ved henting av identer for person", ex)
            return emptyList()
        }

        return response.fold(::onSuccess, ::onFailure)
    }
}

open class GraphQLConfig(
    val scope: String,
    val url: String,
    val additionalHeaders: List<Pair<String, String>> = emptyList()
)

interface GraphQLClient<T : Any, R : Any> {
    suspend fun query(req: T): Result<R>

    fun HttpMessageBuilder.additionalHeaders(additionalHeaders: List<Pair<String, String>>) {
        additionalHeaders.map { (key, value) ->
            header(key, value)
        }
    }
}

class PdlClient(
    config: AzureConfig,
    private val graphQLConfig: GraphQLConfig
) : GraphQLClient<PdlRequest, PdlResponse> {
    private val httpClient = HttpClientFactory.createClient()
    private val tokenProvider = AzureAdTokenProvider(config, graphQLConfig.scope)

    override suspend fun query(req: PdlRequest): Result<PdlResponse> {
        return runCatching {
            httpClient.post(graphQLConfig.url) {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                bearerAuth(tokenProvider.getClientCredentialToken())
                additionalHeaders(graphQLConfig.additionalHeaders)
                setBody(req)
            }
        }.map {
            it.body()
        }
    }
}

private const val ident = "\$ident"

private val IDENT_QUERY = """
    query($ident: ID!) {

        hentIdenter(ident: $ident, historikk: true) {
            identer {
                ident,
                historisk,
                gruppe
            }
        }
    }
""".trimIndent()

data class PdlRequest(
    val query: String,
    val variables: IdentVariables
)

data class IdentVariables(
    val ident: String
)

data class PdlResponse(
    val data: PdlData?,
    val errors: List<GraphQLError>?,
    val extensions: GraphQLExtensions?
)

data class PdlData(
    val hentIdenter: PdlIdenter?,
)

data class PdlIdenter(
    val identer: List<PdlIdent>
)

data class PdlIdent(
    val ident: String,
    val historisk: Boolean,
    val gruppe: PdlGruppe
)

enum class PdlGruppe {
    FOLKEREGISTERIDENT,
    AKTORID,
    NPID,
}

data class GraphQLError(
    val message: String,
    val locations: List<GraphQLErrorLocation>,
    val path: List<String>?,
    val extensions: GraphQLErrorExtension
)

data class GraphQLErrorExtension(
    val code: String?,
    val classification: String
)

data class GraphQLErrorLocation(
    val line: Int?,
    val column: Int?
)

data class GraphQLExtensions(
    val warnings: List<GraphQLWarning>?
)

class GraphQLWarning(
    val query: String?,
    val id: String?,
    val code: String?,
    val message: String?,
    val details: String?,
)

private val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLog")
