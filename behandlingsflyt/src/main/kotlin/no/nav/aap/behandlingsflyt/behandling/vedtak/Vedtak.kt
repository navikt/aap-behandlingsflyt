package no.nav.aap.behandlingsflyt.behandling.vedtak

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDateTime

data class Vedtak(
    val behandlingId: BehandlingId,
    val vedtakstidspunkt: LocalDateTime
)
