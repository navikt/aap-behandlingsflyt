package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.filterNotNull
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

data class SykdomGrunnlag(
    val yrkesskadevurdering: Yrkesskadevurdering?,
    /**
     * Alle tidligere vedtatte sykdomsvurderinger + nye sykdomsvurderinger for inneværende behandling. 
     * Skal sjeldent brukes direkte
     */
    val sykdomsvurderinger: List<Sykdomsvurdering>,
) {

    fun yrkesskadevurdringTidslinje(periode: Periode): Tidslinje<Yrkesskadevurdering> {
        return tidslinjeOf(periode to yrkesskadevurdering).filterNotNull()
    }

    fun somSykdomsvurderingstidslinje(
        maksDato: LocalDate = Tid.MAKS
    ): Tidslinje<Sykdomsvurdering> {
        return filtrertSykdomstidslinje(maksDato) { true }
    }
    
    fun gjeldendeSykdomsvurderinger(maksDato: LocalDate = Tid.MAKS): List<Sykdomsvurdering> {
        return somSykdomsvurderingstidslinje(maksDato).segmenter().map { it.verdi }
    }

    fun sykdomsvurderingerVurdertIBehandling(behandlingId: BehandlingId): List<Sykdomsvurdering> {
        return sykdomsvurderinger.filter { it.vurdertIBehandling == behandlingId }
    }
    
    fun historiskeSykdomsvurderinger(behandlingIdForGrunnlag: BehandlingId): List<Sykdomsvurdering> {
        return sykdomsvurderinger.filterNot { it.vurdertIBehandling == behandlingIdForGrunnlag }
    }

    fun vedtattSykdomstidslinje(
        behandlingId: BehandlingId,
        maksDato: LocalDate = Tid.MAKS
    ): Tidslinje<Sykdomsvurdering> {
        return filtrertSykdomstidslinje(maksDato) { it.vurdertIBehandling != behandlingId }
    }

    fun gjeldendeVedtatteSykdomsvurderinger(
        behandlingId: BehandlingId,
        maksDato: LocalDate = Tid.MAKS
    ): List<Sykdomsvurdering> {
        return vedtattSykdomstidslinje(behandlingId, maksDato).segmenter().map { it.verdi }
    }

    private fun filtrertSykdomstidslinje(
        maksDato: LocalDate = Tid.MAKS,
        filter: (sykdomsvurdering: Sykdomsvurdering) -> Boolean
    ): Tidslinje<Sykdomsvurdering> {
        return sykdomsvurderinger
            .filter(filter)
            .groupBy { it.vurdertIBehandling }
            .values
            .sortedBy { it[0].opprettet }
            .flatMap { it.sortedBy { it.vurderingenGjelderFra } }
            .somTidslinje { Periode(it.vurderingenGjelderFra, it.vurderingenGjelderTil ?: maksDato) }
            .komprimer()
    }
}
