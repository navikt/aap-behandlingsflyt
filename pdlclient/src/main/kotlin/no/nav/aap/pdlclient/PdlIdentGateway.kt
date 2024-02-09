package no.nav.aap.pdlclient

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.aap.HttpClientFactory
import no.nav.aap.ktor.client.AzureAdTokenProvider
import no.nav.aap.ktor.client.AzureConfig


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
