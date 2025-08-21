package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode

interface KanTriggeRevurdering {
    fun behovForRevurdering(behandlingId: BehandlingId): List<VurderingsbehovMedPeriode>
}