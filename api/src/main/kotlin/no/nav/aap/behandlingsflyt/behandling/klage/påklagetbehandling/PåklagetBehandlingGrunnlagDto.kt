package no.nav.aap.behandlingsflyt.behandling.klage.påklagetbehandling

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetVedtakType
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.prosessering.statistikk.tilKontraktVurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class PåklagetBehandlingGrunnlagDto(
    val behandlinger: List<BehandlingMedVedtakDto>,
    val gjeldendeVurdering: PåklagetBehandlingVurderingDto?,
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurdertAv: VurdertAvResponse?
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
)

internal fun BehandlingMedVedtak.tilBehandlingMedVedtakDto() =
    BehandlingMedVedtakDto(
        saksnummer = saksnummer.toString(),
        referanse = referanse.referanse,
        typeBehandling = typeBehandling,
        status = status,
        opprettetTidspunkt = opprettetTidspunkt,
        vedtakstidspunkt = vedtakstidspunkt,
        virkningstidspunkt = virkningstidspunkt,
        vurderingsbehov = vurderingsbehov.tilKontraktVurderingsbehov(),
        årsakTilOpprettelse = årsakTilOpprettelse
    )

