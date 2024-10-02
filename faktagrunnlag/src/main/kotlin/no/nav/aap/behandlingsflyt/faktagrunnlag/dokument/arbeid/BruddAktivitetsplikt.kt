package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Type.IKKE_MØTT_TIL_ANNEN_AKTIVITET
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Type.IKKE_MØTT_TIL_BEHANDLING
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Type.IKKE_MØTT_TIL_MØTE
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Type.IKKE_MØTT_TIL_TILTAK
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Type.IKKE_SENDT_INN_DOKUMENTASJON
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.sakogbehandling.NavIdent
import no.nav.aap.verdityper.sakogbehandling.SakId
import java.time.LocalDateTime
import java.util.*

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

/** Representerer fakta om ett enkelt brudd (§§ 11-7 til 11-9) for én periode. */
data class BruddAktivitetsplikt(
    /** Intern id brukt i databasen. Skal ikke deles utenfor appen. */
    val id: BruddAktivitetspliktId,

    /** Ekstern id. Skal brukes hvis entiteten sendes ut av appen. */
    val hendelseId: HendelseId,

    /** Knytter sammen flere brudd som ble rapportert inn av saksbehandler som én handling. */
    val innsendingId: InnsendingId,

    val navIdent: NavIdent,

    val sakId: SakId,
    val type: Type,
    val paragraf: Paragraf,
    val begrunnelse: String,
    val periode: Periode,
    val opprettetTid: LocalDateTime,

    /* TODO: Fjern default når det er avklart hvilke grunner vi skal ha. */
    /* TODO: Legg på persistering når det er avklart. */
    val grunn: Grunn = Grunn.INGEN_GYLDIG_GRUNN,
) {

    enum class Type {
        IKKE_MØTT_TIL_MØTE,
        IKKE_MØTT_TIL_BEHANDLING,
        IKKE_MØTT_TIL_TILTAK,
        IKKE_MØTT_TIL_ANNEN_AKTIVITET,
        IKKE_SENDT_INN_DOKUMENTASJON,
        IKKE_AKTIVT_BIDRAG;
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

    val relevantFor_11_8: Boolean
        get() = this.type in bruddTyperRelevantFor_11_8

    val gyldigGrunnFor_11_8: Boolean
        get() = this.grunn in gyldigeGrunnerFor_11_8

    val relevantFor_11_9: Boolean
        get() = this.type in bruddTyperRelevantFor_11_9

    val gyldigGrunnFor_11_9: Boolean
        get() = this.grunn in gyldigeGrunnerFor_11_9

    companion object {
        private val bruddTyperRelevantFor_11_8 = listOf(
            IKKE_MØTT_TIL_BEHANDLING,
            IKKE_MØTT_TIL_TILTAK,
            IKKE_MØTT_TIL_ANNEN_AKTIVITET,
        )

        private val gyldigeGrunnerFor_11_8 = listOf(
            Grunn.SYKDOM_ELLER_SKADE,
            Grunn.STERKE_VELFERDSGRUNNER,
        )


        private val bruddTyperRelevantFor_11_9 = listOf(
            IKKE_MØTT_TIL_MØTE,
            IKKE_MØTT_TIL_BEHANDLING,
            IKKE_MØTT_TIL_TILTAK,
            IKKE_MØTT_TIL_ANNEN_AKTIVITET,
            IKKE_SENDT_INN_DOKUMENTASJON,
        )

        private val gyldigeGrunnerFor_11_9 = listOf(
            Grunn.SYKDOM_ELLER_SKADE, /* TODO: bekreft at "sykdom eller skade" kan regnes som rimelig grunn. */
            Grunn.STERKE_VELFERDSGRUNNER, /* TODO: bekreft at "sterke veldferdsgrunner" kan regnes som rimelig grunn. */
            Grunn.RIMELIG_GRUNN,
        )
    }
}


