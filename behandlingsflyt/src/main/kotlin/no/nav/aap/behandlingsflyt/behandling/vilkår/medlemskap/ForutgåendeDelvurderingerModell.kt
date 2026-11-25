package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDate

/** Vurdering av forutgående medlemskap for vinduet [[relevantDato] - 5 år, [relevantDato]]. */
data class ForutgåendeDelvurderingerGrunnlag(
    val kanBehandlesAutomatisk: Boolean,
    val tilhørighetVurdering: List<TilhørighetVurdering>,
    val relevantDato: LocalDate,
    val vurdertIBehandling: BehandlingId,
) {
    fun vurdering(): KanBehandlesAutomatiskVurdering =
        KanBehandlesAutomatiskVurdering(kanBehandlesAutomatisk, tilhørighetVurdering)
}
