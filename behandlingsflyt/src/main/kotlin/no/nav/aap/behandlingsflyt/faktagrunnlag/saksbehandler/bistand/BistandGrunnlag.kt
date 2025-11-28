package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

data class BistandGrunnlag(
    /**
     * Nye vurderinger for inneværende behandling + vedtatte vurderinger
     */
    val vurderinger: List<Bistandsvurdering>,
) {
    fun somBistandsvurderingstidslinje(
        maksDato: LocalDate = Tid.MAKS
    ): Tidslinje<Bistandsvurdering> {
        return filtrertBistandstidslinje(maksDato) { true }
    }

    fun gjeldendeBistandsvurderinger(maksDato: LocalDate = Tid.MAKS): List<Bistandsvurdering> {
        return somBistandsvurderingstidslinje(maksDato).segmenter().map { it.verdi }
    }

    fun bistandsvurderingerVurdertIBehandling(behandlingId: BehandlingId): List<Bistandsvurdering> {
        return vurderinger.filter { it.vurdertIBehandling == behandlingId }
    }

    fun historiskeBistandsvurderinger(behandlingIdForGrunnlag: BehandlingId): List<Bistandsvurdering> {
        return vurderinger.filterNot { it.vurdertIBehandling == behandlingIdForGrunnlag }
    }

    fun vedtattBistandstidslinje(
        behandlingId: BehandlingId,
        maksDato: LocalDate = Tid.MAKS
    ): Tidslinje<Bistandsvurdering> {
        return filtrertBistandstidslinje(maksDato) { it.vurdertIBehandling != behandlingId }
    }

    fun gjeldendeVedtatteBistandsvurderinger(
        behandlingId: BehandlingId,
        maksDato: LocalDate = Tid.MAKS
    ): List<Bistandsvurdering> {
        return vedtattBistandstidslinje(behandlingId, maksDato).segmenter().map { it.verdi }
    }

    private fun filtrertBistandstidslinje(
        maksDato: LocalDate = Tid.MAKS,
        filter: (bistandsvurdering: Bistandsvurdering) -> Boolean
    ): Tidslinje<Bistandsvurdering> {
        return vurderinger
            .filter(filter)
            .groupBy { it.vurdertIBehandling }
            .values
            .sortedBy { it[0].opprettet }
            .flatMap { vurderingerForBehandling -> vurderingerForBehandling.sortedBy { it.vurderingenGjelderFra } }
            .somTidslinje { Periode(it.vurderingenGjelderFra, maksDato) }
            .komprimer()
    }
}
