package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.httpklient.auth.Bruker
import java.util.UUID

data class PåklagetBehandlingVurderingLøsningDto(
    val påklagetBehandling: UUID?,
    val påklagetVedtakType: PåklagetVedtakType
) {
    fun tilVurdering(bruker: Bruker, behandlingId: BehandlingId?) = PåklagetBehandlingVurdering(
        påklagetBehandling = behandlingId,
        påklagetVedtakType = påklagetVedtakType,
        vurdertAv = bruker.ident
    )

}