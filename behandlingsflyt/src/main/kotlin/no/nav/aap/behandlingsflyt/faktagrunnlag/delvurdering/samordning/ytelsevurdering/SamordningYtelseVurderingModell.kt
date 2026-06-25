package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.behandling.samordning.AvklaringsType
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere.slåSammenTilListe
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import java.time.LocalDateTime

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
)

data class SamordningVurderingGrunnlag(
    val vurderingerId: Long? = null,
    val begrunnelse: String?,
    val vurderinger: Set<SamordningVurdering>,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime
) {
    fun tilTidslinje(): Tidslinje<List<Pair<Ytelse, SamordningVurderingPeriode>>> {
        return vurderinger.filter { it.ytelseType.type == AvklaringsType.MANUELL }
            .map { ytelse ->
                val segmenterForYtelse =
                    ytelse.vurderingPerioder.map { Segment(it.periode, Pair(ytelse.ytelseType, it)) }
                Tidslinje(segmenterForYtelse)
            }.fold(Tidslinje.empty()) { acc, curr ->
                acc.kombiner(curr, slåSammenTilListe())
            }
    }
}

interface SamordningPeriode {
    val periode: Periode
}