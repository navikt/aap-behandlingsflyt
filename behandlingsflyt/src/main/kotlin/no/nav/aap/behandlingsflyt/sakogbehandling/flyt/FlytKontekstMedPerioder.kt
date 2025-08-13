package no.nav.aap.behandlingsflyt.sakogbehandling.flyt

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.type.Periode

/**
 * Kontekst for behandlingen som inneholder hvilke perioder som er til vurdering for det enkelte steget som skal vurderes
 * Orkestratoren beriker objektet med periodene slik at det følger av reglene for periodisering for de enkelte typene behandlingene
 */
data class FlytKontekstMedPerioder(
    val sakId: SakId,
    val behandlingId: BehandlingId,
    val forrigeBehandlingId: BehandlingId?,
    val behandlingType: TypeBehandling,
    val vurderingType: VurderingType,
    val rettighetsperiode: Periode,
    val vurderingsbehovRelevanteForSteg: Set<Vurderingsbehov>
) {
    fun harNoeTilBehandling(): Boolean {
        return vurderingType != VurderingType.IKKE_RELEVANT
    }

    fun erFørstegangsbehandlingEllerRevurdering(): Boolean {
        return vurderingType in setOf(VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING)
    }

    fun erFørstegangsbehandling(): Boolean {
        return vurderingType == VurderingType.FØRSTEGANGSBEHANDLING
    }

    fun erRevurderingMedVurderingsbehov(behov: Vurderingsbehov): Boolean {
        return vurderingType == VurderingType.REVURDERING
                && vurderingsbehovRelevanteForSteg.contains(behov)
    }
}