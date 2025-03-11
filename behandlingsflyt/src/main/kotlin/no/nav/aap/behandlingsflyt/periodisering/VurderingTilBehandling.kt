package no.nav.aap.behandlingsflyt.periodisering

import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.komponenter.type.Periode

data class VurderingTilBehandling(
    val vurderingType: VurderingType,
    val rettighetsperiode: Periode,
    val forlengelsePeriode: Periode? = null,
    val årsakerTilBehandling: Set<ÅrsakTilBehandling>
) {
    init {
        if (vurderingType == VurderingType.FORLENGELSE) {
            requireNotNull(forlengelsePeriode) { "Forlengenseperiode er påkrevd ved forlengelse" }
        }
    }

    fun skalVurdereNoe(): Boolean {
        return vurderingType != VurderingType.IKKE_RELEVANT
    }
}
