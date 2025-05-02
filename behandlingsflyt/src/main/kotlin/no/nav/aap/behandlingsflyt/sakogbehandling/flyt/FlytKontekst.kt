package no.nav.aap.behandlingsflyt.sakogbehandling.flyt

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId

data class FlytKontekst(
    val sakId: SakId,
    val behandlingId: BehandlingId,
    val forrigeBehandlingId: BehandlingId?,
    val behandlingType: TypeBehandling
)