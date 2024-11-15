package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.InnsendingId
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.AvslåttLegeerklæringId
import no.nav.aap.verdityper.dokument.JournalpostId

data class MottattDokumentReferanse(
    val type: Type,
    val verdi: String,
) {
    enum class Type {
        JOURNALPOST,
        BRUDD_AKTIVITETSPLIKT_INNSENDING_ID,
        AVSLÅTT_LEGEERKLÆRING_ID
    }

    @get:JsonIgnore
    val asJournalpostId: JournalpostId get() = JournalpostId(verdi).also {
        require(type == Type.JOURNALPOST)
    }

    @get:JsonIgnore
    val asInnsendingId: InnsendingId get() = InnsendingId(verdi).also {
        require(type == Type.BRUDD_AKTIVITETSPLIKT_INNSENDING_ID)
    }

    @get:JsonIgnore
    val asAvslåttLegeerklæringId: AvslåttLegeerklæringId get() = AvslåttLegeerklæringId(verdi).also {
        require(type == Type.AVSLÅTT_LEGEERKLÆRING_ID)
    }

    constructor(id: InnsendingId): this(Type.BRUDD_AKTIVITETSPLIKT_INNSENDING_ID, id.asString)
    constructor(id: JournalpostId): this(Type.JOURNALPOST, id.identifikator)
    constructor(id: AvslåttLegeerklæringId): this(Type.AVSLÅTT_LEGEERKLÆRING_ID, id.asString)
}