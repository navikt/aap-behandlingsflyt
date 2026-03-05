package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Tid
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.YearMonth

data class BarnepensjonGrunnlag(
    val vurdering: BarnepensjonVurdering
) {
    fun tilTidslinje(): Tidslinje<Beløp> {
        return Tidslinje(
            this.vurdering.perioder
                .map { it.tilSegment() })
    }
}

data class BarnepensjonVurdering(
    val begrunnelse: String,
    val perioder: Set<BarnepensjonPeriode>,
    val vurdertIBehandling: BehandlingId,
    val vurdertAv: Bruker,
    val opprettet: Instant,
)

data class BarnepensjonPeriode(
    val fom: YearMonth,
    val tom: YearMonth?,
    val månedsats: Beløp
) {
    init {
        if (månedsats.verdi < BigDecimal.ZERO) {
            throw IllegalArgumentException("Månedbeløp kan ikke være negativt")
        }
    }

    fun dagsats(): Beløp {
        return Beløp(månedsats.multiplisert(12 / 260).verdi().setScale(0, RoundingMode.HALF_UP))
    }

    fun tilSegment(): Segment<Beløp> {
        return Segment(Periode(fom.atDay(1), tom?.atEndOfMonth() ?: Tid.MAKS), dagsats())
    }
}
