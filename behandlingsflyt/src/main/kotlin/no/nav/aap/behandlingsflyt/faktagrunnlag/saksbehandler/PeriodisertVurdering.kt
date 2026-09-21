package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler

import no.nav.aap.behandlingsflyt.erSystembruker
import no.nav.aap.behandlingsflyt.dokumentasjon.Blokker
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Tid
import java.time.Instant
import java.time.LocalDate

interface PeriodisertVurdering {
    val fom: LocalDate
    val tom: LocalDate?
    val vurdertIBehandling: BehandlingId
    val opprettet: Instant
    val vurdertAv: Bruker

    fun erAutomatiskVurdert(): Boolean = vurdertAv.erSystembruker()

    /* Lag markup av innholdet til denne vurderingen for generering av PDF.
    * Det skal *ikke* genereres markup for [fom], [tom], [vurdertIBehandling], [opprettet] og [vurdertAv].
    **/
    fun genererDokumentasjon(): Blokker
}

fun <T: PeriodisertVurdering> List<T>.gjeldendeVurderinger(): Tidslinje<T> {
    return this.groupBy { it.vurdertIBehandling }
        .values
        .sortedBy { it[0].opprettet }
        .flatMap { it.sortedBy { vurdering -> vurdering.fom } }
        .somTidslinje { Periode(it.fom, it.tom ?: Tid.MAKS) }
        .komprimer()
}