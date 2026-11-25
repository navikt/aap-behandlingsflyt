package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav

import no.nav.aap.behandlingsflyt.erSystembruker
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@JvmInline
value class Kravreferanse(val verdi: UUID) {
    companion object {
        fun ny(): Kravreferanse = Kravreferanse(UUID.randomUUID())
    }
}

sealed interface KravVurdering {
    val referanse: Kravreferanse
    val journalpostId: JournalpostId?
    val vurdertAv: Bruker
    val begrunnelse: String
    val vurdertIBehandling: BehandlingId
    val opprettet: Instant

    fun erAutomatiskVurdert(): Boolean {
        return vurdertAv.erSystembruker()
    }

    fun forJournalpostId(journalpostId: JournalpostId): Boolean {
        return this.journalpostId == journalpostId
    }

    fun forJournalpostId(journalpostId: String): Boolean {
        return this.journalpostId?.identifikator == journalpostId
    }
}

/**
 * Et krav som gir mulig rett til AAP fra [muligRettFra]. Kravet stammer enten fra en søknad eller fra en
 * sak migrert fra Arena. Bruk `when` over subtypene når du trenger felt som bare finnes på én av dem.
 */
sealed interface RelevantKrav : KravVurdering {
    val muligRettFra: LocalDate
}

data class RelevantSøknadKrav(
    override val referanse: Kravreferanse,
    override val journalpostId: JournalpostId,
    override val vurdertAv: Bruker,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,

    val søknadsdato: Søknadsdato,
    val overstyrMuligRettFra: OverstyrMuligRettFra?,
    override val muligRettFra: LocalDate,
) : RelevantKrav

data class TrukketSøknad(
    override val referanse: Kravreferanse,
    override val journalpostId: JournalpostId,
    override val vurdertAv: Bruker,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,
) : KravVurdering

data class Klage(
    override val referanse: Kravreferanse,
    override val journalpostId: JournalpostId,
    override val vurdertAv: Bruker,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,
) : KravVurdering

data class Tilleggsopplysning(
    override val referanse: Kravreferanse,
    override val journalpostId: JournalpostId,
    override val vurdertAv: Bruker,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,
) : KravVurdering

data class RelevantMigrertKrav(
    override val referanse: Kravreferanse,
    override val vurdertAv: Bruker,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,

    val virkningstidspunktArena: LocalDate,
    override val muligRettFra: LocalDate,
    val arenaSaksnummer: String,
    val rettighetstype: MigrertRettighetstype,
    val resterendeKvoteOrdinær: Int,
) : RelevantKrav {
    override val journalpostId: JournalpostId? = null
}

enum class MigrertRettighetstype {
    ORDINÆR,
    UNNTAK_11_12_ÅR_4,
    UNNTAK_11_12_ÅR_5,
    SP_ERSTATNING_11_13,
}

enum class KravType {
    RELEVANT_KRAV,
    TRUKKET_SØKNAD,
    KLAGE,
    TILLEGGSOPPLYSNING,
    MIGRERT_KRAV
}

data class OverstyrMuligRettFra(val dato: LocalDate, val årsak: OverstyrMuligRettFraÅrsak, val begrunnelse: String)
data class Søknadsdato(val dato: LocalDate, val årsak: SøknadsdatoÅrsak, val begrunnelse: String)

enum class SøknadsdatoÅrsak {
    BrukerHarSøktTidligere,
    FeilregistrertSøknadsdato,
    SøknadMottatt
}

enum class OverstyrMuligRettFraÅrsak {
    IkkeIStandTilÅSøkeTidligere,
    MisvisendeOpplysninger,
    Ukjent // Utført før årsak ble lagt inn, skal ikke brukes på nye ting
}
