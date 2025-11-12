package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import java.time.LocalDateTime
import java.util.*

data class BehandlinginfoDTO(
    val referanse: UUID,
    val type: String,
    val status: Status,
    val vurderingsbehov: List<Vurderingsbehov>,
    val årsakTilOpprettelse: ÅrsakTilOpprettelse?,
    val opprettet: LocalDateTime
)
