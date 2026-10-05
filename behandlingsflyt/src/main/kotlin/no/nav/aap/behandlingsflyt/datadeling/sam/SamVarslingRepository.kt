package no.nav.aap.behandlingsflyt.datadeling.sam

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.repository.Repository
import java.time.LocalDateTime

/**
 * Logg over vurderinger av om SAM skal varsles om et vedtak, og resultatet av eventuelle kall.
 * Brukes for å kunne dokumentere hva vi har sendt til SAM.
 */
interface SamVarslingRepository : Repository {
    fun lagre(behandlingId: BehandlingId, varsling: SamVarsling)
    fun hent(behandlingId: BehandlingId): List<LagretSamVarsling>
}

data class SamVarsling(
    val vedtakId: Long,
    val varslet: Boolean,
    val førstegangsbehandling: Boolean,
    val endringIRettighetstype: Boolean,
    val antallTpYtelser: Int,
    val request: SamordneVedtakRequest,
    val respons: SamordneVedtakRespons?,
)

data class LagretSamVarsling(
    val varsling: SamVarsling,
    val opprettetTid: LocalDateTime,
)
