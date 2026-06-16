package no.nav.aap.behandlingsflyt.behandling.krav

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Gjenopptak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Klage
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.NyttKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.OverstyrMuligRettFra
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Tilleggsopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.TrukketSøknad
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.Instant
import java.time.LocalDate

data class KravGrunnlagDto(
    val harTilgangTilÅSaksbehandle: Boolean,
    val nyeVurderinger: List<KravVurderingDto>,
    val vedtatteVurderinger: List<KravVurderingDto>
)

sealed interface KravVurderingDto {
    val type: KravType
    val journalpostId: JournalpostId
    val vurdertAv: Bruker
    val begrunnelse: String
    val vurdertIBehandling: BehandlingId
    val opprettet: Instant
}

interface KravMedDatoDto {
    val søknadsdato: Søknadsdato
    val overstyrMuligRettFra: OverstyrMuligRettFra?
    val muligRettFra: LocalDate
}

data class NyttKravDto(
    override val journalpostId: JournalpostId,
    override val vurdertAv: Bruker,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,

    override val søknadsdato: Søknadsdato,
    override val overstyrMuligRettFra: OverstyrMuligRettFra?,
    override val muligRettFra: LocalDate,
) : KravMedDatoDto, KravVurderingDto {
    override val type: KravType = KravType.NYTT_KRAV_AAP
}

data class TrukketSøknadDto(
    override val journalpostId: JournalpostId,
    override val vurdertAv: Bruker,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,
) : KravVurderingDto {
    override val type: KravType = KravType.TRUKKET_SØKNAD
}

data class GjenopptakDto(
    override val journalpostId: JournalpostId,
    override val vurdertAv: Bruker,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,

    override val søknadsdato: Søknadsdato,
    override val overstyrMuligRettFra: OverstyrMuligRettFra?,
    override val muligRettFra: LocalDate,
) : KravMedDatoDto, KravVurderingDto {
    override val type: KravType = KravType.GJENOPPTAK
}

data class KlageDto(
    override val journalpostId: JournalpostId,
    override val vurdertAv: Bruker,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,
) : KravVurderingDto {
    override val type: KravType = KravType.KLAGE

}

data class TilleggsopplysningDto(
    override val journalpostId: JournalpostId,
    override val vurdertAv: Bruker,
    override val begrunnelse: String,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,
) : KravVurderingDto {
    override val type: KravType = KravType.TILLEGGSOPPLYSNING
}

fun KravVurdering.somDto(): KravVurderingDto = when (this) {
    is NyttKrav -> NyttKravDto(
        journalpostId = this.journalpostId,
        vurdertAv = this.vurdertAv,
        begrunnelse = this.begrunnelse,
        vurdertIBehandling = this.vurdertIBehandling,
        opprettet = this.opprettet,
        søknadsdato = this.søknadsdato,
        overstyrMuligRettFra = this.overstyrMuligRettFra,
        muligRettFra = this.muligRettFra,
    )

    is TrukketSøknad -> TrukketSøknadDto(
        journalpostId = this.journalpostId,
        vurdertAv = this.vurdertAv,
        begrunnelse = this.begrunnelse,
        vurdertIBehandling = this.vurdertIBehandling,
        opprettet = this.opprettet,
    )

    is Gjenopptak -> GjenopptakDto(
        journalpostId = this.journalpostId,
        vurdertAv = this.vurdertAv,
        begrunnelse = this.begrunnelse,
        vurdertIBehandling = this.vurdertIBehandling,
        opprettet = this.opprettet,
        søknadsdato = this.søknadsdato,
        overstyrMuligRettFra = this.overstyrMuligRettFra,
        muligRettFra = this.muligRettFra,
    )

    is Klage -> KlageDto(
        journalpostId = this.journalpostId,
        vurdertAv = this.vurdertAv,
        begrunnelse = this.begrunnelse,
        vurdertIBehandling = this.vurdertIBehandling,
        opprettet = this.opprettet,
    )

    is Tilleggsopplysning -> TilleggsopplysningDto(
        journalpostId = this.journalpostId,
        vurdertAv = this.vurdertAv,
        begrunnelse = this.begrunnelse,
        vurdertIBehandling = this.vurdertIBehandling,
        opprettet = this.opprettet,
    )
}