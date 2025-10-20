package no.nav.aap.behandlingsflyt.behandling.oppholdskrav

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.utils.Validation
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
        .map { it.tilTidslinje() }
        .fold(Tidslinje()) { acc, other ->
            acc.kombiner(other, StandardSammenslåere.prioriterHøyreSideCrossJoin())
        }

data class OppholdskravVurdering(
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val vurdertAv: String,
    val vurdertIBehandling: BehandlingId,
    val perioder: List<OppholdskravPeriode>,
) {
    fun tilTidslinje(): Tidslinje<OppholdakravTidslinjeData> {
        return Tidslinje(
            perioder.sortedBy { it.fom }
                .map { periode ->
                    Segment(
                        periode = Periode(fom = periode.fom, tom = periode.tom ?: LocalDate.MAX),
                        verdi = OppholdakravTidslinjeData(
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

fun Tidslinje<OppholdakravTidslinjeData>.validerGyldigForRettighetsperiode(rettighetsperiode: Periode): Validation<Tidslinje<OppholdakravTidslinjeData>> {
    val periodeForVurdering = helePerioden()

    if (!erSammenhengende()) {
        return Validation.Invalid(this, "Periodene for oppholdskrav er ikke sammenhengende")
    }

    if(periodeForVurdering.fom > rettighetsperiode.fom) {
        return Validation.Invalid(this, "Det er ikke tatt stilling til hele rettighetsperioden. Rettisgetsperioden for saken starter ${rettighetsperiode.fom} mens vurderingens første periode starter ${periodeForVurdering.fom}. ")
    }

    if(periodeForVurdering.tom < rettighetsperiode.tom) {
        return Validation.Invalid(this, "Det er ikke tatt stilling til hele rettighetsperioden. Rettisgetsperioden for saken slutter ${rettighetsperiode.tom} mens vurderingens siste periode slutter ${periodeForVurdering.tom}. ")
    }

    return Validation.Valid(this)
}

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