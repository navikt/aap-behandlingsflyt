package no.nav.aap.etterlevelse.sporing

import com.fasterxml.jackson.databind.JsonNode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje
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
    val input: JsonNode,
    val operasjoner: Tidslinje<Operasjon>,
) {
    enum class Operasjon {
        FJERNET,
        LAGT_TIL,
        ENDRET,
        UENDRET,
    }
}
