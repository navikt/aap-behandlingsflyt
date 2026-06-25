package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import java.time.LocalDateTime

data class VurderingsbehovOgÅrsak(
    val vurderingsbehov: List<VurderingsbehovMedPeriode>,
    val årsak: ÅrsakTilOpprettelse,
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val beskrivelse: String? = null
)