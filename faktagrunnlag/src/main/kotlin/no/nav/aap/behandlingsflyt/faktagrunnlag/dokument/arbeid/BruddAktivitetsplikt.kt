package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Paragraf.*
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.sakogbehandling.NavIdent
import no.nav.aap.verdityper.sakogbehandling.SakId
import java.time.Instant
import java.util.*

/** Representerer fakta om ett enkelt brudd (§§ 11-7 til 11-9) for én periode. */
class BruddAktivitetsplikt(
    val dokumenttype: Dokumenttype,

    /** Intern id brukt i databasen. Skal ikke deles utenfor appen. */
    val id: BruddAktivitetspliktId,

    /** Ekstern id. Skal brukes hvis entiteten sendes ut av appen. */
    val hendelseId: HendelseId,

    /** Knytter sammen flere brudd som ble rapportert inn av saksbehandler som én handling. */
    val innsendingId: InnsendingId,

    val sakId: SakId,

    /** Saksbehandler/veileder som sendte inn dokumentet. */
    val innsender: NavIdent,

    /** Tidspunktet dokumentet ble mottatt. */
    val opprettetTid: Instant,

    /** De dagene hvor det har vært et brudd. */
    val periode: Periode,

    val brudd: Brudd,

    val paragraf: Paragraf,

    val begrunnelse: String,

    val grunn: Grunn,
) {
    init {
        require(paragraf in brudd.gyldigeParagrafer) {
            "$paragraf kan ikke brukes ved aktivitetspliktbruddet $brudd"
        }
    }

    enum class Dokumenttype {
        BRUDD,
        FEILREGISTRERING,
    }

    enum class Brudd(val gyldigeParagrafer: Collection<Paragraf>) {
        IKKE_MØTT_TIL_MØTE(listOf(PARAGRAF_11_9)),
        IKKE_MØTT_TIL_BEHANDLING(listOf(PARAGRAF_11_8, PARAGRAF_11_9)),
        IKKE_MØTT_TIL_TILTAK(listOf(PARAGRAF_11_8, PARAGRAF_11_9)),
        IKKE_MØTT_TIL_ANNEN_AKTIVITET(listOf(PARAGRAF_11_8)),
        IKKE_SENDT_INN_DOKUMENTASJON(listOf(PARAGRAF_11_9)),
        IKKE_AKTIVT_BIDRAG(listOf(PARAGRAF_11_7));
    }

    enum class Grunn {
        SYKDOM_ELLER_SKADE,
        STERKE_VELFERDSGRUNNER,
        RIMELIG_GRUNN,
        INGEN_GYLDIG_GRUNN,
    }

    enum class Paragraf {
        PARAGRAF_11_7,
        PARAGRAF_11_8,
        PARAGRAF_11_9
    }
}

data class BruddAktivitetspliktId(internal val id: Long)

data class InnsendingId(@JsonValue val value: UUID) {
    val asString get() = value.toString()

    constructor(value: String) : this(UUID.fromString(value))

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

