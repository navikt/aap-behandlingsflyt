package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.flate

import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class PåklagetBehandlingGrunnlagDto(
    val behandlinger: List<BehandlingMedVedtakDto>
)

data class BehandlingMedVedtakDto(
    val referanse: UUID,
    val typeBehandling: TypeBehandling,
    val status: Status,
    val opprettetTidspunkt: LocalDateTime,
    val vedtakstidspunkt: LocalDateTime,
    val virkningstidspunkt: LocalDate?,
)

internal fun BehandlingMedVedtak.tilBehandlingMedVedtakDto() =
    BehandlingMedVedtakDto(
        referanse = referanse.referanse,
        typeBehandling = typeBehandling,
        status = status,
        opprettetTidspunkt = opprettetTidspunkt,
        vedtakstidspunkt = vedtakstidspunkt,
        virkningstidspunkt = virkningstidspunkt,
    )    


