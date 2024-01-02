package no.nav.aap.behandlingsflyt.faktagrunnlag.bistand

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.bistand.BistandVurdering
import no.nav.aap.behandlingsflyt.behandling.BehandlingId

class BistandGrunnlag(
    val id: Long,
    val behandlingId: BehandlingId,
    val vurdering: BistandVurdering,
)
