package no.nav.aap.behandlingsflyt.kontrakt.hendelse

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.verdityper.dokument.JournalpostId
import java.util.*

public data class InnsendingReferanse(
    val type: Type,
    val verdi: String,
) {
    public enum class Type {
        JOURNALPOST,
        BRUDD_AKTIVITETSPLIKT_INNSENDING_ID,
        AVVIST_LEGEERKLÆRING_ID,
        REVURDERING_ID,
        BEHANDLING_REFERANSE,
        MANUELL_OPPRETTELSE
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
    val asBehandlingReferanse: BehandlingReferanse get() = BehandlingReferanse(UUID.fromString(verdi)).also {
        require(type == Type.BEHANDLING_REFERANSE)
    }

    @get:JsonIgnore
    val asAvvistLegeerklæringId: AvvistLegeerklæringId get() = AvvistLegeerklæringId(verdi).also {
        require(type == Type.AVVIST_LEGEERKLÆRING_ID)
    }

    public constructor(id: InnsendingId): this(Type.BRUDD_AKTIVITETSPLIKT_INNSENDING_ID, id.asString)
    public constructor(id: JournalpostId): this(Type.JOURNALPOST, id.identifikator)
    public constructor(id: AvvistLegeerklæringId): this(Type.AVVIST_LEGEERKLÆRING_ID, id.asString)
}

public data class InnsendingId(@JsonValue val value: UUID) {
    val asString: String get() = value.toString()

    public constructor(value: String) : this(UUID.fromString(value))

    public companion object {
        public fun ny(): InnsendingId = InnsendingId(UUID.randomUUID())
    }
}

public data class AvvistLegeerklæringId(@JsonValue val value: UUID) {
    val asString: String get() = value.toString()

    public constructor(value: String) : this(UUID.fromString(value))

    public companion object {
        public fun ny(): AvvistLegeerklæringId = AvvistLegeerklæringId(UUID.randomUUID())
    }
}