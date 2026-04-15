package no.nav.aap.behandlingsflyt.behandling.vedtak

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDate
import java.time.LocalDateTime

/** ID for vedtak.
 *
 * Vi deler vanligvis ikke database-ID-ene utenfor behandlingsflyt, men bruker referanser. I dette tilfellet
 * så er vedtak-id delt utenfor appen, gjennom api-intern og gjennom SAM.
 */
@JvmInline
value class VedtakId(val id: Long)

data class Vedtak(
    val id: VedtakId,
    val behandlingId: BehandlingId,
    val vedtakstidspunkt: LocalDateTime,
    val virkningstidspunkt: LocalDate?,
)
