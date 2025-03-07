package no.nav.aap.behandlingsflyt.behandling.vedtak

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDateTime

class VedtakService(
    val vedtakRepository: VedtakRepository
) {
    fun lagreVedtak(behandlingId: BehandlingId, vedtakstidspunkt: LocalDateTime) {
        vedtakRepository.lagre(behandlingId, vedtakstidspunkt)
    }
}
