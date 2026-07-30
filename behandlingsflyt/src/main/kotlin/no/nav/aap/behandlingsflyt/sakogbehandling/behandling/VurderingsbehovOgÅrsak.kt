package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDateTime

data class VurderingsbehovOgÅrsak(
    val vurderingsbehov: List<VurderingsbehovMedPeriode>,
    val årsak: ÅrsakTilOpprettelse,
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val opprettetAv: Bruker? = null,
    val beskrivelse: String? = null
)