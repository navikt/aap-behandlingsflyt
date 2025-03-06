package no.nav.aap.behandlingsflyt.behandling.vedtak

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDateTime

interface VedtakRepository {
    fun lagre(behandlingId: BehandlingId, vedtakstidspunkt:LocalDateTime)
    fun hent(): Vedtak?
}