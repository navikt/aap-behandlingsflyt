package no.nav.aap.behandlingsflyt.sakogbehandling.flyt

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.periodisering.VurderingTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId

/**
 * Kontekst for behandlingen som inneholder hvilke perioder som er til vurdering for det enkelte steget som skal vurderes
 * Orkestratoren beriker objektet med periodene slik at det følger av reglene for periodisering for de enkelte typene behandlingene
 */
data class FlytKontekstMedPerioder(
    val sakId: SakId,
    val behandlingId: BehandlingId,
    val forrigeBehandlingId: BehandlingId?,
    val behandlingType: TypeBehandling,
    val vurdering: VurderingTilBehandling
) {
    fun harNoeTilBehandling(): Boolean {
        return vurdering.skalVurdereNoe()
    }

    fun erFørstegangsbehandlingEllerRevurdering(): Boolean {
        return vurdering.vurderingType in setOf(VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING)
    }

    fun erFørstegangsbehandling(): Boolean {
        return vurdering.vurderingType == VurderingType.FØRSTEGANGSBEHANDLING
    }

    fun erRevurderingMedÅrsak(årsak: ÅrsakTilBehandling): Boolean {
        return vurdering.vurderingType == VurderingType.REVURDERING
                && vurdering.årsakerTilBehandling.contains(årsak)
    }
}