package no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv

import com.fasterxml.jackson.annotation.JsonCreator

// Docs: https://confluence.adeo.no/display/BOA/opprettJournalpost
data class Journalpost(
    val journalposttype: Journalposttype,
    val avsenderMottaker: AvsenderMottaker? = null,
    val bruker: DokarkivBruker? = null,
    val tema: Tema? = null,
    val behandlingstema: String? = null,
    val tittel: String? = null,
    val kanal: String? = null,
    val journalfoerendeEnhet: String? = null,
    val eksternReferanseId: String? = null,
    val datoMottatt: String? = null,
    val tilleggsopplysninger: List<Tilleggsopplysning>? = null,
    val sak: DokarkivSak? = null,
    val dokumenter: List<Dokument>? = null
)

data class AvsenderMottaker(
    val id: String,
    val idType: AvsenderIdType,
    val navn: String? = null,
    val land: String? = null
)

data class DokarkivBruker(
    val id: String,
    val idType: BrukerIdType
)

data class Tilleggsopplysning(
    val nokkel: String,
    val verdi: String
)

data class DokarkivSak(
    val sakstype: Sakstype,
    val fagsakId: String? = null,
    val fagsaksystem: FagsaksSystem? = null
)

data class Dokument(
    val tittel: String? = null,
    val brevkode: String? = null,
    val dokumentKategori: String? = null,
    val dokumentvarianter: List<DokumentVariant> = mutableListOf()
)

data class DokumentVariant(
    val filtype: Filetype,
    val variantformat: Variantformat,
    val fysiskDokument: ByteArray,
    val filnavn: String? = null,
    val batchnavn: String? = null
)

enum class Journalposttype {
    NOTAT
}

// Se https://confluence.adeo.no/display/BOA/Tema
enum class Tema(val tittel: String) {
    AAP("Arbeidsavklaringspenger"),
}

enum class AvsenderIdType {
    FNR,
}

enum class BrukerIdType {
    FNR,
}

enum class Sakstype {
    FAGSAK,
    GENERELL_SAK,
    ARKIVSAK
}

enum class FagsaksSystem {
    AO01, // Arena
    KELVIN,
}

// Se https://confluence.adeo.no/display/BOA/Filtype
enum class Filetype {
    PDF,
    JSON,
}

// Se https://confluence.adeo.no/display/BOA/Variantformat
enum class Variantformat {
    ARKIV,
    ORIGINAL,
}

// Se https://confluence.adeo.no/display/BOA/opprettJournalpost
data class JournalpostResponse(
    val journalpostId: Long, // Dokumentasjon sier at dette feltet er String. Men det ser ut at vi får numerisk ID her
    val melding: String? = null,
    val journalpostferdigstilt: Boolean,
    val dokumenter: List<DokumentInfo>
)

data class DokumentInfo @JsonCreator() constructor(
    val dokumentInfoId: Long, // Dokumentasjon sier at dette feltet er String. Men det ser ut at vi får numerisk ID her
)
