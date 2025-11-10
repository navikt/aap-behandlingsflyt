package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate
import java.time.LocalDateTime

data class SamordningUføreGrunnlag(
    val vurdering: SamordningUføreVurdering,
)

data class SamordningUføreVurdering(
    val begrunnelse: String,
    val vurderingPerioder: List<SamordningUføreVurderingPeriode>,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime? = null
) {
    fun tilTidslinje(): Tidslinje<Prosent> {
        val sorterteVurderinger = vurderingPerioder.sortedBy { it.virkningstidspunkt }
        val sisteElement = sorterteVurderinger.lastOrNull()

        return if (sisteElement == null) {
            Tidslinje.empty()
        } else

            sorterteVurderinger.zipWithNext { gjeldende, neste ->
                Segment(
                    periode = Periode(
                        gjeldende.virkningstidspunkt,
                        neste.virkningstidspunkt.minusDays(1)
                    ), verdi = gjeldende.uføregradTilSamordning
                )
            }.plus(Segment(Periode(sisteElement.virkningstidspunkt, Tid.MAKS), sisteElement.uføregradTilSamordning)).let(::Tidslinje)
    }
}

data class SamordningUføreVurderingPeriode(
    val virkningstidspunkt: LocalDate,
    val uføregradTilSamordning: Prosent
)

data class SamordningUføreVurderingDto(
    val begrunnelse: String,
    val vurderingPerioder: List<SamordningUføreVurderingPeriodeDto>,
)

data class SamordningUføreVurderingPeriodeDto(
    val virkningstidspunkt: LocalDate,
    val uføregradTilSamordning: Int
)
