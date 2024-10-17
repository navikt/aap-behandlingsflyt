package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.kontrakt.brev.Status
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.util.UUID

class Brevbestilling(
    val id: Long,
    val behandlingId: BehandlingId,
    val typeBrev: TypeBrev,
    val referanse: UUID,
    val status: Status,
)