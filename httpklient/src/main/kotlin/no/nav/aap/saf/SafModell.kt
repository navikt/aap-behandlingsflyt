package no.nav.aap.saf

import no.nav.aap.pdl.GraphQLError
import no.nav.aap.pdl.GraphQLExtensions

data class SafRequest(val query: String, val variables: Variables) {
    data class Variables(val fagsakId: String)
}

abstract class SafResponse(
    val errors: List<GraphQLError>?,
    val extensions: GraphQLExtensions?
)

class SafDokumentoversiktFagsakDataResponse(
    val data: SafDokumentversiktFagsakData?,
    errors: List<GraphQLError>?,
    extensions: GraphQLExtensions?
) : SafResponse(errors, extensions)

data class Dokumentvariant(val variantformat: Variantformat)
data class Dokument(
    val dokumentInfoId: String,
    val tittel: String,
    val brevkode: String? /* TODO: enum */,
    val dokumentvarianter: List<Dokumentvariant>
)

data class Journalpost(val journalpostId: String, val dokumenter: List<Dokument>)
data class DokumentoversiktFagsak(val journalposter: List<Journalpost>)
data class SafDokumentversiktFagsakData(val dokumentoversiktFagsak: DokumentoversiktFagsak?)
enum class Variantformat {
    ARKIV, SLADDET, ORIGINAL
}