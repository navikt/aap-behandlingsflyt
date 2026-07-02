package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.Instant
import java.time.LocalDate

interface PeriodisertVurdering {
    val fom: LocalDate
    val tom: LocalDate?
    val vurdertIBehandling: BehandlingId
    val opprettet: Instant
}

data class EnkelPeriodisertVurdering(
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant
) : PeriodisertVurdering

fun List<PeriodisertVurdering>.gjeldendeVurderinger(): Tidslinje<PeriodisertVurdering> {
    return this.groupBy { it.vurdertIBehandling }
        .values
        .sortedBy { it[0].opprettet }
        .flatMap { it.sortedBy { it.fom } }
        .somTidslinje { Periode(it.fom, it.tom ?: Tid.MAKS) }
        .komprimer()
}