package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import java.time.LocalDateTime

interface Variables

data class SafRequest(val query: String, val variables: Variables)

data class DokumentoversiktFagsakVariables(val fagsakId: String) : Variables

data class DokumentoversiktBrukerVariables(
    val brukerId: BrukerId,
    val tema: List<String>,
    val journalposttyper: List<Journalposttype>,
    val journalstatuser: List<Journalstatus>,
    val foerste: Int,
    val etter: String? = null,
) : Variables

data class BrukerId(
    val id: String,
    val type: String,
) {
    override fun toString(): String = "BrukerId(id=, type=$type)"
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

class SafDokumentoversiktBrukerDataResponse(
    val data: SafDokumentversiktBrukerData?,
    errors: List<GraphQLError>?,
    extensions: GraphQLExtensions?
) : SafResponse(errors, extensions)

data class Dokumentvariant(
    val variantformat: Variantformat,
    val saksbehandlerHarTilgang: Boolean,
)

data class DokumentInfo(
    val dokumentInfoId: String,
    val tittel: String,
    val brevkode: String? /* TODO: enum */,
    val dokumentvarianter: List<Dokumentvariant>
)

data class Journalpost(
    val journalpostId: String,
    val dokumenter: List<DokumentInfo>,
    val tittel: String?,
    val journalposttype: Journalposttype,
    val journalstatus: Journalstatus,
    val tema: String?,
    val temanavn: String?,
    val behandlingstema: String?,
    val behandlingstemanavn: String?,
    val sak: JournalpostSak?,
    val avsenderMottaker: AvsenderMottaker?,
    val datoOpprettet: LocalDateTime?,
    val relevanteDatoer: List<RelevantDato>?
)

data class JournalpostSak(
    val sakstype: Type,
    val tema: String?,
    val fagsaksystem: String?,
    val fagsakId: String?
) {
    enum class Type {
        FAGSAK,
        GENERELL_SAK,
    }
}

data class AvsenderMottaker(
    val id: String?,
    val type: String?,
    val navn: String?
)

data class RelevantDato(
    val dato: LocalDateTime,
    val datotype: String
)

data class DokumentoversiktFagsak(val journalposter: List<Journalpost>)
data class SafDokumentversiktFagsakData(val dokumentoversiktFagsak: DokumentoversiktFagsak?)

data class DokumentoversiktBruker(val journalposter: List<Journalpost>)
data class SafDokumentversiktBrukerData(val dokumentoversiktBruker: DokumentoversiktBruker?)

enum class Variantformat {
    ARKIV, SLADDET, ORIGINAL, PRODUKSJON, FULLVERSJON
}

enum class Journalposttype {
    I, U, N
}

enum class Journalstatus {
    MOTTATT,
    JOURNALFOERT,
    FERDIGSTILT,
    EKSPEDERT,
    UNDER_ARBEID,
    FEILREGISTRERT,
    UTGAAR,
    AVBRUTT,
    UKJENT_BRUKER,
    RESERVERT,
    OPPLASTING_DOKUMENT,
    UKJENT
}