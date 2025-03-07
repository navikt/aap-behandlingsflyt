package no.nav.aap.behandlingsflyt.behandling.vedtak

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import java.time.LocalDateTime

interface VedtakRepository : Repository {
    fun lagre(behandlingId: BehandlingId, vedtakstidspunkt: LocalDateTime)
    fun hent(behandlingId: BehandlingId): Vedtak?
}