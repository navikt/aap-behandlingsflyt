package no.nav.aap.behandlingsflyt.faktagrunnlag.uføre

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.uføre.Uføre
import no.nav.aap.behandlingsflyt.behandling.BehandlingId

class UføreGrunnlag(
    val id: Long,
    val behandlingId: BehandlingId,
    val vurdering: Uføre,
)
