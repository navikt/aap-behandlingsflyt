package no.nav.aap.behandlingsflyt.behandling.oppholdskrav

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Tid
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class OppholdskravGrunnlag(
    val vurderinger: List<OppholdskravVurdering>,
    val opprettet: LocalDateTime = LocalDateTime.now(),
) {
    fun tidslinje(): Tidslinje<OppholdskravTidslinjeData> {
        return vurderinger.tilTidslinje()
    }

    fun somPeriodiserteVurderinger(): List<OppholdskravPeriodisertVurdering> {
        return vurderinger.flatMap { vurdering ->
            vurdering.perioder.map { periode ->
                OppholdskravPeriodisertVurdering(
                    fom = periode.fom,
                    tom = periode.tom,
                    land = periode.land,
                    oppfylt = periode.oppfylt,
                    begrunnelse = periode.begrunnelse,
                    vurdertAv = vurdering.vurdertAv,
                    vurdertIBehandling = vurdering.vurdertIBehandling,
                    opprettetTid = vurdering.opprettet,
                )
            }
        }
    }
}

fun List<OppholdskravVurdering>.tilTidslinje(): Tidslinje<OppholdskravTidslinjeData> =
    this.sortedBy { it.opprettet }
        .map { it.tilTidslinje() }
        .fold(Tidslinje()) { acc, other ->
            acc.kombiner(other, StandardSammenslåere.prioriterHøyreSideCrossJoin())
        }


data class OppholdskravVurdering(
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val vurdertAv: Bruker,
    val vurdertIBehandling: BehandlingId,
    val perioder: List<OppholdskravPeriode>,
) {
    fun tilTidslinje(): Tidslinje<OppholdskravTidslinjeData> {
        return Tidslinje(
            perioder.sortedBy { it.fom }
                .map { periode ->
                    Segment(
                        periode = Periode(fom = periode.fom, tom = periode.tom ?: Tid.MAKS),
                        verdi = OppholdskravTidslinjeData(
                            land = periode.land,
                            opprettet = opprettet,
                            oppfylt = periode.oppfylt,
                            begrunnelse = periode.begrunnelse,
                            vurdertAv = vurdertAv
                        )
                    )
                }
        )
    }
}

data class OppholdskravPeriode(
    val fom: LocalDate,
    val tom: LocalDate?,
    val land: String?,
    val oppfylt: Boolean,
    val begrunnelse: String,
)

data class OppholdskravTidslinjeData(
    val land: String?,
    val oppfylt: Boolean,
    val begrunnelse: String,
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val vurdertAv: Bruker,
)

data class OppholdskravPeriodisertVurdering(
    override val fom: LocalDate,
    override val tom: LocalDate?,
    val land: String?,
    val oppfylt: Boolean,
    val begrunnelse: String,
    val vurdertAv: Bruker,
    override val vurdertIBehandling: BehandlingId,
    val opprettetTid: LocalDateTime,
) : PeriodisertVurdering {
    override val opprettet: Instant = opprettetTid.atZone(ZoneId.of("Europe/Oslo")).toInstant()
}