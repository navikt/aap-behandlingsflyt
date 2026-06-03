package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.Instant
import java.time.LocalDate

sealed interface KravVurdering {
    val journalpostId: JournalpostId
    val vurdertAv: String
    val begrunnelse: String
    val vurdertIBehandling: BehandlingId
    val opprettet: Instant
}

data class NyttKrav(
    override val journalpostId: JournalpostId,
    override val vurdertAv: String,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,

    val søknadsdato: Søknadsdato,
    val muligRettFra: MuligRettFra?,
    val kravdato: LocalDate,
) : KravVurdering

data class TrukketSøknad(
    override val journalpostId: JournalpostId,
    override val vurdertAv: String,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,
) : KravVurdering

data class Gjenopptak(
    override val journalpostId: JournalpostId,
    override val vurdertAv: String,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,

    val søknadsdato: Søknadsdato,
    val muligRettFra: MuligRettFra?,
    val kravdato: LocalDate,
) : KravVurdering

data class Klage(
    override val journalpostId: JournalpostId,
    override val vurdertAv: String,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,
) : KravVurdering

data class Tilleggsopplysning(
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

data class MuligRettFra(val dato: LocalDate, val årsak: MuligRettFraÅrsak)
data class Søknadsdato(val dato: LocalDate, val årsak: SøknadsdatoÅrsak)

enum class SøknadsdatoÅrsak {
    BrukerHarSøktTidligere,
    FeilregistrertSøknadsdato,
    JournalpostMottatt
}

enum class MuligRettFraÅrsak {
    IkkeIStandTilÅSøkeTidligere,
    MisvisendeOpplysninger,
}
