package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

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
    
    val soknadsdato: LocalDate,
    val soknadsdatoÅrsak: SøknadsdatoÅrsak?,
    val muligRettFra: LocalDate?,
    val muligRettFraÅrsak: MuligRettFraÅrsak?,
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

    val soknadsdato: LocalDate?,
    val soknadsdatoÅrsak: SøknadsdatoÅrsak?,
    val muligRettFra: LocalDate?,
    val muligRettFraÅrsak: MuligRettFraÅrsak?,
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
    TRUKKET_SOKNAD,
    GJENOPPTAK,
    KLAGE,
    TILLEGGSOPPLYSNING,
}

enum class SøknadsdatoÅrsak {
    BrukerHarSøktTidligere,
    FeilregistrertSøknadsdato,
}

enum class MuligRettFraÅrsak {
    IkkeIStandTilÅSøkeTidligere,
    MisvisendeOpplysninger,
}
