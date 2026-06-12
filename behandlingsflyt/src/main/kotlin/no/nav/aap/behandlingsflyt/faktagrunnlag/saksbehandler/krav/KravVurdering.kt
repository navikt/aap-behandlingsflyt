package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.Instant
import java.time.LocalDate

sealed interface KravVurdering {
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
    val muligRettFra: MuligRettFra?
    val kravdato: LocalDate
}

data class NyttKrav(
    override val journalpostId: JournalpostId,
    override val vurdertAv: Bruker,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,

    override val søknadsdato: Søknadsdato,
    override val muligRettFra: MuligRettFra?,
    override val kravdato: LocalDate,
) : KravMedDato, KravVurdering

data class TrukketSøknad(
    override val journalpostId: JournalpostId,
    override val vurdertAv: Bruker,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,
) : KravVurdering

data class Gjenopptak(
    override val journalpostId: JournalpostId,
    override val vurdertAv: Bruker,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,

    override val søknadsdato: Søknadsdato,
    override val muligRettFra: MuligRettFra?,
    override val kravdato: LocalDate,
) : KravMedDato, KravVurdering

data class Klage(
    override val journalpostId: JournalpostId,
    override val vurdertAv: Bruker,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,
) : KravVurdering

data class Tilleggsopplysning(
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

data class MuligRettFra(val dato: LocalDate, val årsak: MuligRettFraÅrsak)
data class Søknadsdato(val dato: LocalDate, val årsak: SøknadsdatoÅrsak)

enum class SøknadsdatoÅrsak {
    BrukerHarSøktTidligere,
    FeilregistrertSøknadsdato,
    SøknadMottatt
}

enum class MuligRettFraÅrsak {
    IkkeIStandTilÅSøkeTidligere,
    MisvisendeOpplysninger,
}
