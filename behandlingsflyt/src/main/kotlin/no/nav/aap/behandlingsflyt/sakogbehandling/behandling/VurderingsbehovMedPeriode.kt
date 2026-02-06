package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDateTime

data class VurderingsbehovMedPeriode(
    val type: Vurderingsbehov,
    val periode: Periode? = null,
    val oppdatertTid: LocalDateTime = LocalDateTime.now()
)
