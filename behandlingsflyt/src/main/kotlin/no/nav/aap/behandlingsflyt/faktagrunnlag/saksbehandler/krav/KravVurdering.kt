package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
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
    val journalpostId: JournalpostId
    val vurdertAv: Bruker
    val begrunnelse: String
    val vurdertIBehandling: BehandlingId
    val opprettet: Instant

    fun erAutomatiskVurdert(): Boolean {
        return vurdertAv == SYSTEMBRUKER

    }
}

interface KravMedDato {
    val søknadsdato: Søknadsdato
    val overstyrMuligRettFra: OverstyrMuligRettFra?
    val muligRettFra: LocalDate
}

data class NyttKrav(
    override val referanse: Kravreferanse,
    override val journalpostId: JournalpostId,
    override val vurdertAv: Bruker,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,

    override val søknadsdato: Søknadsdato,
    override val overstyrMuligRettFra: OverstyrMuligRettFra?,
    override val muligRettFra: LocalDate,
) : KravVurdering, KravMedDato

data class TrukketSøknad(
    override val referanse: Kravreferanse,
    override val journalpostId: JournalpostId,
    override val vurdertAv: Bruker,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,
) : KravVurdering

data class Gjenopptak(
    override val referanse: Kravreferanse,
    override val journalpostId: JournalpostId,
    override val vurdertAv: Bruker,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,

    override val søknadsdato: Søknadsdato,
    override val overstyrMuligRettFra: OverstyrMuligRettFra?,
    override val muligRettFra: LocalDate,
) : KravVurdering, KravMedDato

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
