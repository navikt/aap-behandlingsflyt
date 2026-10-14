package no.nav.aap.etterlevelse.sporing

import com.fasterxml.jackson.databind.JsonNode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.Instant


/** En unik nøkkel som identifiserer sporbar kode.
 *
 * Identifiserer f.eks. en bestemt metode som gjør konkrete delvurderinger.
 * */
@JvmInline
value class Sporingsnøkkel(val nøkkel: String)

data class Sporing(
    val behandlingId: BehandlingId,
    val opprettetTidspunkt: Instant,
    val sporingsnøkkel: Sporingsnøkkel,
    val versjon: String,
    val grunnlag: JsonNode,
)
