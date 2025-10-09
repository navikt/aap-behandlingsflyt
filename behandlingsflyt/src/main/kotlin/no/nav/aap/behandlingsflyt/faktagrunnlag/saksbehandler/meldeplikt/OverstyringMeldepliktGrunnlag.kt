package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.LocalDateTime

data class OverstyringMeldepliktGrunnlag(
    val vurderinger: List<OverstyringMeldepliktVurdering>
) {
    fun tilTidslinje(): Tidslinje<OverstyringMeldepliktData> {
        return vurderinger.tilTidslinje()
    }
}

fun List<OverstyringMeldepliktVurdering>.tilTidslinje(): Tidslinje<OverstyringMeldepliktData> =
    this.sortedBy { it.opprettetTid }
        .map { it.tilTidslinje() }
        .fold(Tidslinje()) { acc, tidslinje ->
        acc.outerJoin(tidslinje) { left, right ->
            check(left != null || right != null) {
                "Både left og right kan ikke være NULL når man lager tidslinje for overstyring av meldeplikt. Det tyder på kodefeil."
            }

            OverstyringMeldepliktData(
                vurdertAv = right?.vurdertAv ?: left?.vurdertAv!!,
                vurdertIBehandling = right?.vurdertIBehandling ?: left?.vurdertIBehandling!!,
                opprettetTid = right?.opprettetTid ?: left?.opprettetTid!!,
                begrunnelse = right?.begrunnelse ?: left?.begrunnelse!!,
                meldepliktOverstyringStatus = right?.meldepliktOverstyringStatus ?: left?.meldepliktOverstyringStatus!!
            )
        }
    }

fun OverstyringMeldepliktVurdering.tilTidslinje(): Tidslinje<OverstyringMeldepliktData> {
    return Tidslinje(
        perioder.map { it.tilTidslinjeSegment(vurdertAv, vurdertIBehandling, opprettetTid) }
    )
}

fun OverstyringMeldepliktVurderingPeriode.tilTidslinjeSegment(vurdertAv: String, vurdertIBehandling: BehandlingReferanse, opprettetTid: LocalDateTime?): Segment<OverstyringMeldepliktData> {
    return Segment(
        periode = Periode(fom = fom, tom = tom),
        verdi = OverstyringMeldepliktData(
            begrunnelse = begrunnelse,
            meldepliktOverstyringStatus = meldepliktOverstyringStatus,
            vurdertAv = vurdertAv,
            vurdertIBehandling = vurdertIBehandling,
            opprettetTid = opprettetTid ?: LocalDateTime.now(),
        )
    )
}


enum class MeldepliktOverstyringStatus(val value: String) {
    RIMELIG_GRUNN("RIMELIG_GRUNN"),
    IKKE_MELDT_SEG("IKKE_MELDT_SEG"),
    HAR_MELDT_SEG("HAR_MELDT_SEG");
}

data class OverstyringMeldepliktVurderingPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val begrunnelse: String,
    val meldepliktOverstyringStatus: MeldepliktOverstyringStatus,
)

data class OverstyringMeldepliktVurdering(
    val perioder: List<OverstyringMeldepliktVurderingPeriode>,
    val vurdertIBehandling: BehandlingReferanse,
    val vurdertAv: String,
    val opprettetTid: LocalDateTime?
)

data class OverstyringMeldepliktData(
    val vurdertAv: String,
    val vurdertIBehandling: BehandlingReferanse,
    val opprettetTid: LocalDateTime,
    val begrunnelse: String,
    val meldepliktOverstyringStatus: MeldepliktOverstyringStatus,
)