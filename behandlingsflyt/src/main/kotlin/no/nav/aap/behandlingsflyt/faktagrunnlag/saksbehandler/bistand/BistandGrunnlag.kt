package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode

class BistandGrunnlag(
    val id: Long,
    val behandlingId: BehandlingId,
    val vurdering: BistandVurdering,
) {
    fun harVurdertPeriode(periode: Periode): Boolean {
        return true
    }
}
