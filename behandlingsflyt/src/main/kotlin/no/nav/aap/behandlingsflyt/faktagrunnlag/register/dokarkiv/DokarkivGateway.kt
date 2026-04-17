package no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv

import com.fasterxml.jackson.annotation.JsonCreator
import no.nav.aap.komponenter.gateway.Gateway

interface DokarkivGateway : Gateway {

    fun oppdater(journalpost: Journalpost, forsøkFerdigstill: Boolean): JournalpostResponse

    // Docs: https://confluence.adeo.no/display/BOA/opprettJournalpost
    data class Journalpost(
        val journalposttype: Journalposttype,
        val avsenderMottaker: AvsenderMottaker? = null,
        val bruker: Bruker? = null,
        val tema: Tema? = null,
        val behandlingstema: String? = null,
        val tittel: String? = null,
        val kanal: String? = null,
        val journalfoerendeEnhet: String? = null,
        val eksternReferanseId: String? = null,
        val datoMottatt: String? = null,
        val tilleggsopplysninger: List<Tilleggsopplysning>? = null,
        val sak: Sak? = null,
        val dokumenter: List<Dokument>? = null
    )

    data class AvsenderMottaker(
        val id: String,
        val idType: AvsenderIdType,
        val navn: String? = null,
        val land: String? = null
    )

    data class Bruker(
        val id: String,
        val idType: BrukerIdType
    )

    data class Tilleggsopplysning(
        val nokkel: String,
        val verdi: String
    )

    data class Sak(
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
        UTGAAENDE,
        NOTAT
    }

    // Se https://confluence.adeo.no/display/BOA/Tema
    enum class Tema(val tittel: String) {
        AAP("Arbeidsavklaringspenger"),
    }

    enum class AvsenderIdType {
        FNR,
        ORGNR,
        HPRNR,
        UTL_ORG
    }

    enum class BrukerIdType {
        FNR,
        ORGNR,
        AKTOERID
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
        PDFA,
        XML,
        RTF,
        DLF,
        JPEG,
        TIFF,
        AXML,
        DXML,
        JSON,
        PNG
    }

    // Se https://confluence.adeo.no/display/BOA/Variantformat
    enum class Variantformat {
        ARKIV,
        ORIGINAL,
        SLADDET,
        FULLVERSJON,
        SKANNING_META,
        BREVBESTILLING,
        PRODUKSJON,
        PRODUKSJON_DLF
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
}