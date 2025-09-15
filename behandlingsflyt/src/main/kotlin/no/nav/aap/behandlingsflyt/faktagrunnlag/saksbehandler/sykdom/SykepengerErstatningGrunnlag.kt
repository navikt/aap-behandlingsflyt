package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

data class SykepengerErstatningGrunnlag(
    val id: Long? = null,
    val vurderinger: List<SykepengerVurdering>
) {
    constructor(vurderinger: List<SykepengerVurdering>) : this(null, vurderinger)

    fun somTidslinje(
        kravDato: LocalDate,
        sisteMuligDagMedYtelse: LocalDate
    ): Tidslinje<SykepengerVurdering> {
        return vurderinger.sortedBy { it.vurdertTidspunkt }
            .map { vurdering ->
                val fom = listOfNotNull(vurdering.gjelderFra, kravDato).max()

                Tidslinje(
                    Periode(
                        fom = fom,
                        tom = sisteMuligDagMedYtelse
                    ), vurdering
                )
            }
            .fold(Tidslinje()) { acc, curr ->
                acc.kombiner(curr, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            }

    }
}
