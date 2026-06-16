package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

sealed interface KravVurdering {
    val referanse: UUID
    val journalpostId: JournalpostId
    val vurdertAv: String
    val begrunnelse: String
    val vurdertIBehandling: BehandlingId
    val opprettet: Instant
}

data class NyttKrav(
    override val referanse: UUID,
    override val journalpostId: JournalpostId,
    override val vurdertAv: String,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,

    val søknadsdato: Søknadsdato,
    val overstyrMuligRettFra: OverstyrMuligRettFra?,
    val muligRettFra: LocalDate,
) : KravVurdering

data class TrukketSøknad(
    override val referanse: UUID,
    override val journalpostId: JournalpostId,
    override val vurdertAv: String,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,
) : KravVurdering

data class Gjenopptak(
    override val referanse: UUID,
    override val journalpostId: JournalpostId,
    override val vurdertAv: String,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,

    val søknadsdato: Søknadsdato,
    val overstyrMuligRettFra: OverstyrMuligRettFra?,
    val muligRettFra: LocalDate,
) : KravVurdering

data class Klage(
    override val referanse: UUID,
    override val journalpostId: JournalpostId,
    override val vurdertAv: String,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,
) : KravVurdering

data class Tilleggsopplysning(
    override val referanse: UUID,
    override val journalpostId: JournalpostId,
    override val vurdertAv: String,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,
) : KravVurdering

enum class KravType {
    NYTT_KRAV_AAP,
    TRUKKET_SØKNAD,
    GJENOPPTAK,
    KLAGE,
    TILLEGGSOPPLYSNING,
}

data class OverstyrMuligRettFra(val dato: LocalDate, val årsak: OverstyrMuligRettFraÅrsak)
data class Søknadsdato(val dato: LocalDate, val årsak: SøknadsdatoÅrsak)

enum class SøknadsdatoÅrsak {
    BrukerHarSøktTidligere,
    FeilregistrertSøknadsdato,
    SøknadMottatt
}

enum class OverstyrMuligRettFraÅrsak {
    IkkeIStandTilÅSøkeTidligere,
    MisvisendeOpplysninger,
}
