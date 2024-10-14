package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Paragraf.PARAGRAF_11_7
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Paragraf.PARAGRAF_11_8
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Paragraf.PARAGRAF_11_9
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.sakogbehandling.NavIdent
import no.nav.aap.verdityper.sakogbehandling.SakId
import java.time.Instant
import java.util.*

sealed interface Aktivitetspliktdokument {
    /** Intern id brukt i databasen. Skal ikke deles utenfor appen. */
    val id: BruddAktivitetspliktId

    /** Ekstern id. Skal brukes hvis entiteten sendes ut av appen. */
    val hendelseId: HendelseId

    /** Knytter sammen flere brudd som ble rapportert inn av saksbehandler som én handling. */
    val innsendingId: InnsendingId

    /** Saksbehandler som sendte inn dokumentet. */
    val navIdent: NavIdent

    /** Tidspunktet dokumentet ble mottatt. */
    val opprettetTid: Instant

    val sakId: SakId

    /** De dagene hvor det har vært et brudd. */
    val periode: Periode
}

data class FeilregistrertBrudd(
    override val id: BruddAktivitetspliktId,
    override val hendelseId: HendelseId,
    override val innsendingId: InnsendingId,
    override val navIdent: NavIdent,
    override val sakId: SakId,
    override val opprettetTid: Instant,
    override val periode: Periode
): Aktivitetspliktdokument

/** Representerer fakta om ett enkelt brudd (§§ 11-7 til 11-9) for én periode. */
data class BruddAktivitetsplikt(
    override val id: BruddAktivitetspliktId,
    override val hendelseId: HendelseId,
    override val innsendingId: InnsendingId,
    override val navIdent: NavIdent,
    override val sakId: SakId,
    override val periode: Periode,
    override val opprettetTid: Instant,
    val type: Type,
    val paragraf: Paragraf,
    val begrunnelse: String,

    /* TODO: Fjern default når det er avklart hvilke grunner vi skal ha. */
    /* TODO: Legg på persistering når det er avklart. */
    val grunn: Grunn = Grunn.INGEN_GYLDIG_GRUNN,
): Aktivitetspliktdokument {
    init {
        require(paragraf in type.gyldigeParagrafer) {
            "$paragraf kan ikke brukes ved aktivitetspliktbruddet $type"
        }
    }

    enum class Type(val gyldigeParagrafer: Collection<Paragraf>) {
        IKKE_MØTT_TIL_MØTE(listOf(PARAGRAF_11_9)),
        IKKE_MØTT_TIL_BEHANDLING(listOf(PARAGRAF_11_8, PARAGRAF_11_9)),
        IKKE_MØTT_TIL_TILTAK(listOf(PARAGRAF_11_8, PARAGRAF_11_9)),
        IKKE_MØTT_TIL_ANNEN_AKTIVITET(listOf(PARAGRAF_11_8)),
        IKKE_SENDT_INN_DOKUMENTASJON(listOf(PARAGRAF_11_9)),
        IKKE_AKTIVT_BIDRAG(listOf(PARAGRAF_11_7));
    }

    /** TODO: avklar behov for forskjellige grunner, og hvilke. */
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

