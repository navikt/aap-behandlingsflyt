package no.nav.aap.misc

import java.time.LocalDateTime
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.outerJoin
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.samordning.AvklaringsType
import no.nav.aap.samordning.Ytelse

data class SamordningYtelse(
    val ytelseType: Ytelse,
    val ytelsePerioder: Set<SamordningYtelsePeriode>,
    val kilde: String,
    val saksRef: String? = null,
)

data class SamordningYtelsePeriode(
    override val periode: Periode,
    val gradering: Prosent?,
    val kronesum: Number? = null
) : SamordningPeriode

data class SamordningVurdering(
    val ytelseType: Ytelse,
    val vurderingPerioder: Set<SamordningVurderingPeriode>,
)

data class SamordningVurderingPeriode(
    override val periode: Periode,
    val gradering: Prosent?,
    val kronesum: Number? = null,
    val manuell: Boolean?,
) : SamordningPeriode

data class SamordningYtelseGrunnlag(
    val grunnlagId: Long,
    val ytelser: Set<SamordningYtelse>,
) {
    fun tidslinjeMedSamordningYtelser(): Tidslinje<List<Ytelse>> {
        return this.ytelser.filter { it.ytelseType.type == AvklaringsType.MANUELL }
            .map { ytelse ->
                ytelse.ytelsePerioder.somTidslinje({ it.periode }, { ytelse.ytelseType })
            }
            .outerJoin()
    }
}

data class SamordningVurderingGrunnlag(
    val vurderingerId: Long? = null,
    val begrunnelse: String?,
    val vurderinger: Set<SamordningVurdering>,
    val vurdertAv: Bruker,
    val vurdertTidspunkt: LocalDateTime
) {
    fun vurderingTidslinje(): Tidslinje<List<Pair<Ytelse, SamordningVurderingPeriode>>> {
        return this.vurderinger.filter { it.ytelseType.type == AvklaringsType.MANUELL }
            .map { ytelse -> ytelse.vurderingPerioder.somTidslinje({ it.periode }, { Pair(ytelse.ytelseType, it) }) }
            .outerJoin()
    }
}

interface SamordningPeriode {
    val periode: Periode
}