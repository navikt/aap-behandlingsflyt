package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.behandling.BehandlingType
import no.nav.aap.behandlingsflyt.sak.SakId

data class FlytKontekst(
    val sakId: SakId,
    val behandlingId: BehandlingId,
    val behandlingType: BehandlingType
)

fun tilKontekst(behandling: Behandling): FlytKontekst {
    return FlytKontekst(sakId = behandling.sakId, behandlingId = behandling.id, behandlingType = behandling.type)
}
