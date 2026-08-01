package no.nav.aap.overganguføre

import java.time.LocalDate
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.misc.gjeldendeVurderinger
import no.nav.aap.komponenter.tidslinje.Tidslinje

data class OvergangUføreGrunnlag(
    /**
     * Nye vurderinger for inneværende behandling + vedtatte vurderinger
     */
    val vurderinger: List<OvergangUføreVurdering>,
) {

    fun somOvergangUforevurderingstidslinje(): Tidslinje<OvergangUføreVurdering> {
        return filtrertOvergangUføreTidslinje() { true }
    }

    fun overgangUføreVurderingerVurdertIBehandling(behandlingId: BehandlingId): List<OvergangUføreVurdering> {
        return vurderinger.filter { it.vurdertIBehandling == behandlingId }
    }

    fun historiskeOvergangUføreVurderinger(behandlingIdForGrunnlag: BehandlingId): List<OvergangUføreVurdering> {
        return vurderinger.filterNot { it.vurdertIBehandling == behandlingIdForGrunnlag }
    }

    fun vedtattOvergangUførevurderingstidslinje(
        behandlingId: BehandlingId,
    ): Tidslinje<OvergangUføreVurdering> {
        return filtrertOvergangUføreTidslinje() { it.vurdertIBehandling != behandlingId }
    }

    private fun filtrertOvergangUføreTidslinje(
        filter: (vurdering: OvergangUføreVurdering) -> Boolean
    ): Tidslinje<OvergangUføreVurdering> {
        return vurderinger
            .filter(filter)
            .gjeldendeVurderinger()
    }

    fun kravdatoUføretrygd() : LocalDate? {
        return somOvergangUforevurderingstidslinje()
            .segmenter()
            .firstOrNull()
            ?.fom()
    }
}