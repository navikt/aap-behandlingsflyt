package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import java.time.LocalDateTime

data class VurderingsbehovMedPeriode(
    val type: Vurderingsbehov,
    val oppdatertTid: LocalDateTime = LocalDateTime.now()
)
