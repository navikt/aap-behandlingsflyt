package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

data class GraphQLExtensions(
    val warnings: List<GraphQLWarning>?
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

class GraphQLWarning(
    val query: String?,
    val id: String?,
    val code: String?,
    val message: String?,
    val details: String?,
)
