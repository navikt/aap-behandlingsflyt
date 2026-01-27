package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.Instant
import java.time.LocalDate

data class OvergangUføreGrunnlag(
    /**
     * Nye vurderinger for inneværende behandling + vedtatte vurderinger
     */
    val vurderinger: List<OvergangUføreVurdering>,
) {

    fun somOvergangUforevurderingstidslinje(maksdato: LocalDate = Tid.MAKS): Tidslinje<OvergangUføreVurdering> {
        return filtrertOvergangUføreTidslinje(maksdato) { true }
    }

    fun gjeldendeOvergangUførevurderinger(maksDato: LocalDate = Tid.MAKS): List<OvergangUføreVurdering> {
        return somOvergangUforevurderingstidslinje(maksDato).segmenter().map { it.verdi }
    }

    fun overgangUføreVurderingerVurdertIBehandling(behandlingId: BehandlingId): List<OvergangUføreVurdering> {
        return vurderinger.filter { it.vurdertIBehandling == behandlingId }
    }

    fun historiskeOvergangUføreVurderinger(behandlingIdForGrunnlag: BehandlingId): List<OvergangUføreVurdering> {
        return vurderinger.filterNot { it.vurdertIBehandling == behandlingIdForGrunnlag }
    }

    fun vedtattOvergangUførevurderingstidslinje(
        behandlingId: BehandlingId,
        maksDato: LocalDate = Tid.MAKS
    ): Tidslinje<OvergangUføreVurdering> {
        return filtrertOvergangUføreTidslinje(maksDato) { it.vurdertIBehandling != behandlingId }
    }

    fun gjeldendeVedtatteOvergangUførevurderinger(
        behandlingId: BehandlingId,
        maksDato: LocalDate = Tid.MAKS
    ): List<OvergangUføreVurdering> {
        return vedtattOvergangUførevurderingstidslinje(behandlingId, maksDato).segmenter().map { it.verdi }
    }

    private fun filtrertOvergangUføreTidslinje(
        maksDato: LocalDate = Tid.MAKS,
        filter: (vurdering: OvergangUføreVurdering) -> Boolean
    ): Tidslinje<OvergangUføreVurdering> {
        return vurderinger
            .filter(filter)
            .groupBy { it.vurdertIBehandling }
            .values
            .sortedBy { it[0].opprettet ?: Instant.now() }
            .flatMap { it.sortedBy { it.fom } }
            .somTidslinje { Periode(it.fom, it.tom ?: maksDato) }
            .komprimer()
    }

}
