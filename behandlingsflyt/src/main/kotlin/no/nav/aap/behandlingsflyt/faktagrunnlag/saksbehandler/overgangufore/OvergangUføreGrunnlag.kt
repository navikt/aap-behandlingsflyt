package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore

import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

data class OvergangUføreGrunnlag(
    val id: Long?,
    val vurderinger: List<OvergangUføreVurdering>,
) {

    fun somOvergangUforevurderingstidslinje(startDato: LocalDate): Tidslinje<OvergangUføreVurdering> {
        return vurderinger
            .sortedBy { it.virkningsdato ?: startDato }
            .fold(Tidslinje()) { tidslinje, vurdering ->
                tidslinje.kombiner(
                    Tidslinje(Periode(vurdering.virkningsdato ?: startDato, Tid.MAKS), vurdering),
                    StandardSammenslåere.prioriterHøyreSideCrossJoin()
                )
            }
    }
}
