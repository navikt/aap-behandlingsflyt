package no.nav.aap.behandlingsflyt.integrasjon.util

data class GraphQLResponse<Data>(
    val data: Data?,
    val errors: List<GraphQLError>?,
)