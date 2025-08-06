package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_7
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_8
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_9
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.type.Periode
import java.time.Instant
import java.util.*

/** Informasjon om et brudd.
 *
 *  Data class fordi:
 *  - ønsker equals og hash som sjekker alle feilt
 *  - Trygt på printe (ingen personopplysninger)
 * */
data class Brudd(
    /** De dagene hvor det har vært et brudd. */
    val periode: Periode,

    val bruddType: BruddType,

    val paragraf: Paragraf,
) {
    init {
        require(paragraf in bruddType.gyldigeParagrafer) {
            "$paragraf kan ikke brukes ved aktivitetspliktbruddet $bruddType"
        }
    }

    enum class Paragraf {
        /**
         * Medlemmets aktivitetsplikt.
         */
        PARAGRAF_11_7,

        /**
         * Fravær fra fastsatt aktivitet.
         */
        PARAGRAF_11_8,

        /**
         * Reduksjon av arbeidsavklaringspenger ved brudd på nærmere bestemte aktivitetsplikter.
         */
        PARAGRAF_11_9
    }

    companion object {
        fun nyttBrudd(sak: Sak, periode: Periode, bruddType: BruddType, paragraf: Paragraf): Brudd {
            require(sak.opprettetTidspunkt.toLocalDate() <= periode.fom) {
                "Brudd før søknadstidspunktet(${sak.opprettetTidspunkt.toLocalDate()} kan ikke registreres: brudd-perioden er $periode"
            }
            return Brudd(
                periode = periode,
                bruddType = bruddType,
                paragraf = paragraf,
            )
        }
    }
}

/** Former for brudd beskrevet i §§ 11-7 til 11-9. */
enum class BruddType(val gyldigeParagrafer: Collection<Brudd.Paragraf>) {
    IKKE_MØTT_TIL_MØTE(listOf(PARAGRAF_11_9)),
    IKKE_MØTT_TIL_BEHANDLING_ELLER_UTREDNING(listOf(PARAGRAF_11_8, PARAGRAF_11_9)),
    IKKE_MØTT_TIL_TILTAK(listOf(PARAGRAF_11_8, PARAGRAF_11_9)),
    IKKE_MØTT_TIL_ANNEN_AKTIVITET(listOf(PARAGRAF_11_8)),
    IKKE_SENDT_INN_DOKUMENTASJON(listOf(PARAGRAF_11_9)),
    IKKE_AKTIVT_BIDRAG(listOf(PARAGRAF_11_7));

    fun paragraf(paragraf: Brudd.Paragraf?): Brudd.Paragraf {
        return when (paragraf) {
            null -> requireNotNull(this.gyldigeParagrafer.singleOrNull()) {
                "$this er ikke knyttet til entydig paragraf"
            }

            in this.gyldigeParagrafer -> paragraf
            else -> error("$paragraf er ikke gyldig for $this")
        }
    }
}

/** Dokument fra saksbehandler med opplysninger om et brudd på §§ 11-7 til 11-9. */
sealed interface AktivitetspliktDokument {
    val metadata: Metadata
    val brudd: Brudd

    data class Metadata(
        /** Intern id brukt i databasen. Skal ikke deles utenfor appen. */
        val id: BruddAktivitetspliktId,

        /** Ekstern id. Skal brukes hvis entiteten sendes ut av appen. */
        val hendelseId: HendelseId,

        /** Knytter sammen flere brudd som ble rapportert inn av saksbehandler som én handling. */
        val innsendingId: InnsendingId,

        /** Saksbehandler/veileder som sendte inn dokumentet. */
        val innsender: Bruker,

        /** Tidspunktet dokumentet ble mottatt. */
        val opprettetTid: Instant,
    )
}

/** Representerer dokumentet fra saksbehandler om at et brudd er feil. */
class AktivitetspliktFeilregistrering(
    override val metadata: AktivitetspliktDokument.Metadata,
    override val brudd: Brudd,
    val begrunnelse: String,
) : AktivitetspliktDokument {
    override fun toString(): String {
        return "AktivitetspliktFeilregistrering(begrunnelse='$begrunnelse', metadata=$metadata, brudd=$brudd)"
    }
}

/** Representerer dokumentet fra saksbehanlder om et brudd. */
class AktivitetspliktRegistrering(
    override val metadata: AktivitetspliktDokument.Metadata,
    override val brudd: Brudd,
    val begrunnelse: String,
    val grunn: Grunn,
) : AktivitetspliktDokument {
    override fun toString(): String {
        return "AktivitetspliktRegistrering(begrunnelse='$begrunnelse', metadata=$metadata, brudd=$brudd, grunn=$grunn)"
    }
}


/** Grunner fra §§ 11-7 til 11-9. */
enum class Grunn {
    SYKDOM_ELLER_SKADE,
    STERKE_VELFERDSGRUNNER,
    RIMELIG_GRUNN,
    INGEN_GYLDIG_GRUNN,
    BIDRAR_AKTIVT,
}

data class BruddAktivitetspliktId(val id: Long)


data class HendelseId(val id: UUID) {
    override fun toString() = id.toString()

    companion object {
        fun ny() = HendelseId(UUID.randomUUID())
    }
}

