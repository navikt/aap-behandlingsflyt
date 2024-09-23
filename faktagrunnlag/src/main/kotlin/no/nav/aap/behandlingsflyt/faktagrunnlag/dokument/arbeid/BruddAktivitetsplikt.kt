package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.sakogbehandling.NavIdent
import no.nav.aap.verdityper.sakogbehandling.SakId
import java.time.LocalDateTime
import java.util.*

data class BruddAktivitetspliktId(internal val id: Long)

data class InnsendingId(private val value: UUID) {
    override fun toString() = value.toString()

    companion object {
        fun ny() = InnsendingId(UUID.randomUUID())
    }
}

data class HendelseId(val id: UUID) {
    override fun toString() = id.toString()

    companion object {
        fun ny() = HendelseId(UUID.randomUUID())
    }
}

/** Representerer fakta om ett enkelt brudd for én periode. */
data class BruddAktivitetsplikt(
    /** Intern id brukt i databasen. Skal ikke deles utenfor appen. */
    val id: BruddAktivitetspliktId?,

    /** Ekstern id. Skal brukes hvis entiteten sendes ut av appen. */
    val hendelseId: HendelseId,

    /** Knytter sammen flere brudd som ble rapportert inn av saksbehandler som én handling. Brukes
     * som journalpostId i mottat_dokument. */
    val innsendingId: InnsendingId,

    val navIdent: NavIdent,

    val sakId: SakId,
    val brudd: AktivitetType,
    val paragraf: Paragraf,
    val begrunnelse: String,
    val periode: Periode,
    val opprettetTid: LocalDateTime,
)

enum class Paragraf {
    PARAGRAF_11_7,
    PARAGRAF_11_8,
    PARAGRAF_11_9
}

enum class AktivitetType {
    IKKE_MØTT_TIL_MØTE,
    IKKE_MØTT_TIL_BEHANDLING,
    IKKE_MØTT_TIL_TILTAK,
    IKKE_MØTT_TIL_ANNEN_AKTIVITET,
    IKKE_SENDT_INN_DOKUMENTASJON,
    IKKE_AKTIVT_BIDRAG
}
