package no.nav.aap.behandlingsflyt.behandling.klage.påklagetbehandling

import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.Tilbakekrevingsbehandling
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurderingerMetaResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.KlagebehandlingMedVedtaksdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetVedtakType
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class PåklagetBehandlingGrunnlagDto(
    val behandlinger: List<BehandlingMedVedtakDto>,
    val vedtatteKlagebehandlinger: List<KlagebehandlingDto>,
    val avsluttaTilbakekrevingsbehandlinger: List<AvsluttaTilbakekrevingsbehandlingDto>,
    val gjeldendeVurdering: PåklagetBehandlingVurderingDto?,
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurderingerMeta: VurderingerMetaResponse,
)

data class PåklagetBehandlingVurderingDto(
    val påklagetBehandling: UUID?,
    val påklagetVedtakType: PåklagetVedtakType
)

data class BehandlingMedVedtakDto(
    val saksnummer: String,
    val referanse: UUID,
    val typeBehandling: TypeBehandling,
    val status: Status,
    val opprettetTidspunkt: LocalDateTime,
    val vedtakstidspunkt: LocalDateTime,
    val virkningstidspunkt: LocalDate?,
    val vurderingsbehov: Set<Vurderingsbehov>,
    val årsakTilOpprettelse: ÅrsakTilOpprettelse?
) {
    companion object {
        fun fraDomene(behandlingMedVedtak: BehandlingMedVedtak): BehandlingMedVedtakDto {
            return BehandlingMedVedtakDto(
                saksnummer = behandlingMedVedtak.saksnummer.toString(),
                referanse = behandlingMedVedtak.referanse.referanse,
                typeBehandling = behandlingMedVedtak.typeBehandling,
                status = behandlingMedVedtak.status,
                opprettetTidspunkt = behandlingMedVedtak.opprettetTidspunkt,
                vedtakstidspunkt = behandlingMedVedtak.vedtakstidspunkt,
                virkningstidspunkt = behandlingMedVedtak.virkningstidspunkt,
                vurderingsbehov = behandlingMedVedtak.vurderingsbehov,
                årsakTilOpprettelse = behandlingMedVedtak.årsakTilOpprettelse
            )
        }
    }
}

data class KlagebehandlingDto(
    val saksnummer: String,
    val referanse: UUID,
    val vedtaksdato: LocalDate
) {
    companion object {
        fun fraDomene(klagebehandlingMedVedtaksdato: KlagebehandlingMedVedtaksdato, saksnummer: Saksnummer): KlagebehandlingDto {
            return KlagebehandlingDto(
                saksnummer = saksnummer.toString(),
                referanse = klagebehandlingMedVedtaksdato.behandling.referanse.referanse,
                vedtaksdato = klagebehandlingMedVedtaksdato.vedtaksdato
            )
        }
    }
}

data class AvsluttaTilbakekrevingsbehandlingDto(
    val saksnummer: String,
    val referanse: String,
    val typeBehandling: TypeBehandling,
    val opprettetTidspunkt: LocalDateTime,
    val vedtaksdato: LocalDate?,
    val eksternSaksbehandlingUrl: String? = null
) {
    companion object {
        fun fraDomene(tilbakekrevingsbehandling: Tilbakekrevingsbehandling): AvsluttaTilbakekrevingsbehandlingDto {
            return AvsluttaTilbakekrevingsbehandlingDto(
                saksnummer = tilbakekrevingsbehandling.eksternFagsakId,
                referanse = tilbakekrevingsbehandling.tilbakekrevingBehandlingId.toString(),
                typeBehandling = TypeBehandling.Tilbakekreving,
                opprettetTidspunkt = tilbakekrevingsbehandling.sakOpprettet,
                vedtaksdato = tilbakekrevingsbehandling.vedtaksdato,
                eksternSaksbehandlingUrl = tilbakekrevingsbehandling.saksbehandlingURL?.toString()
            )
        }
    }
}

