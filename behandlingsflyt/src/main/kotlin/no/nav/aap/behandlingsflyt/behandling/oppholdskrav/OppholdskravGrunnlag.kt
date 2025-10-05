package no.nav.aap.behandlingsflyt.behandling.oppholdskrav

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.LocalDateTime


data class OppholdskravGrunnlag(
    val vurderinger: List<OppholdskravVurdering>,
    val opprettet: LocalDateTime = LocalDateTime.now(),
) {
    fun tidslinje(): Tidslinje<OppholdakravTidslinjeData> {
        return vurderinger.tilTidslinje()
    }
}

fun List<OppholdskravVurdering>.tilTidslinje(): Tidslinje<OppholdakravTidslinjeData> =
    this.sortedBy { it.opprettet }
        .map { vurdering ->
            Tidslinje(
                vurdering.perioder
                    .sortedBy { it.fom }
                    .map { periode ->
                        Segment(
                            periode = Periode(fom = periode.fom, tom = periode.tom ?: LocalDate.MAX),
                            verdi = OppholdakravTidslinjeData(
                                land = periode.land,
                                opprettet = vurdering.opprettet,
                                oppfylt = periode.oppfylt,
                                begrunnelse = periode.begrunnelse,
                                vurdertAv = vurdering.vurdertAv
                            )
                        )
                    }
            )
        }
        .fold(Tidslinje()) { acc, other ->
            acc.kombiner(other, StandardSammenslåere.prioriterHøyreSideCrossJoin())
        }

data class OppholdskravVurdering(
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val vurdertAv: String,
    val vurdertIBehandling: BehandlingId,
    val perioder: List<OppholdskravPeriode>,
)

data class OppholdskravPeriode(
    val fom: LocalDate,
    val tom: LocalDate?,
    val land: String?,
    val oppfylt: Boolean,
    val begrunnelse: String,
)

data class OppholdakravTidslinjeData(
    val land: String?,
    val oppfylt: Boolean,
    val begrunnelse: String,
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val vurdertAv: String,
)