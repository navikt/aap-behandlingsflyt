package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class SykepengerErstatningGrunnlag(
    val vurderinger: List<SykepengerVurdering>
) {
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
