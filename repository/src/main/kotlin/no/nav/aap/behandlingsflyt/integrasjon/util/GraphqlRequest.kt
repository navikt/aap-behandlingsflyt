package no.nav.aap.behandlingsflyt.integrasjon.util

data class GraphqlRequest<Variables>(val query: String, val variables: Variables)
