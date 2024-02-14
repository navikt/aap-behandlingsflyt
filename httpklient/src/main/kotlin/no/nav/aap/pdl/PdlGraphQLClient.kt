package no.nav.aap.pdl

import no.nav.aap.GraphQLClient
import no.nav.aap.GraphQLConfig
import no.nav.aap.ktor.client.auth.azure.AzureAdTokenProvider
import no.nav.aap.ktor.client.auth.azure.AzureConfig

class PdlClient(
    azureConfig: AzureConfig,
    private val graphQLConfig: PdlConfig,
    private val azure: AzureAdTokenProvider = AzureAdTokenProvider(azureConfig)
) : GraphQLClient(graphQLConfig) {

    override suspend fun getToken(): String {
        return azure.getClientCredentialToken(graphQLConfig.scope)
    }
}

class PdlConfig(
    val scope: String,
    url: String,
) : GraphQLConfig(
    url = url,
    additionalHeaders = listOf(
        "Nav-Consumer-Id" to "sakogbehandling",
        "TEMA" to "AAP",
    )
)

data class PdlRequest(
    val query: String,
    val variables: IdentVariables
)

data class IdentVariables(
    val ident: String? = null,
    val identer: List<String>? = null
)

data class PdlResponse<T>(
    val data: T?,
    val errors: List<GraphQLError>?,
    val extensions: GraphQLExtensions?
)

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
