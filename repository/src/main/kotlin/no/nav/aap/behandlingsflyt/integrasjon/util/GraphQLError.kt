package no.nav.aap.behandlingsflyt.integrasjon.util

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
