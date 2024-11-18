package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.InnsendingId
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.AvvistLegeerklæringId
import no.nav.aap.verdityper.dokument.JournalpostId

data class MottattDokumentReferanse(
    val type: Type,
    val verdi: String,
) {
    enum class Type {
        JOURNALPOST,
        BRUDD_AKTIVITETSPLIKT_INNSENDING_ID,
        AVVIST_LEGEERKLÆRING_ID
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
    val asAvvistLegeerklæringId: AvvistLegeerklæringId get() = AvvistLegeerklæringId(verdi).also {
        require(type == Type.AVVIST_LEGEERKLÆRING_ID)
    }

    constructor(id: InnsendingId): this(Type.BRUDD_AKTIVITETSPLIKT_INNSENDING_ID, id.asString)
    constructor(id: JournalpostId): this(Type.JOURNALPOST, id.identifikator)
    constructor(id: AvvistLegeerklæringId): this(Type.AVVIST_LEGEERKLÆRING_ID, id.asString)
}