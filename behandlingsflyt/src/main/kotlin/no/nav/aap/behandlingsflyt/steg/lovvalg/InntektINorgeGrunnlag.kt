package no.nav.aap.behandlingsflyt.steg.lovvalg

import java.time.LocalDate
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.misc.Validation

fun List<ManuellVurderingForLovvalgMedlemskap>.tilTidslinje(maksDato: LocalDate = Tid.MAKS): Tidslinje<ManuellVurderingForLovvalgMedlemskap> =
    sortedBy { it.vurdertDato }.somTidslinje { Periode(it.fom, it.tom ?: Tid.MAKS) }
        .komprimer()
        .begrensetTil(Periode(Tid.MIN, maksDato))

fun Tidslinje<ManuellVurderingForLovvalgMedlemskap>.validerGyldigForRettighetsperiode(rettighetsperiode: Periode): Validation<Tidslinje<ManuellVurderingForLovvalgMedlemskap>> {
    val periodeForVurdering = helePerioden()

    if (!erSammenhengende()) {
        return Validation.Invalid(this, "Periodene for lovvalg og medlemskap er ikke sammenhengende")
    }

    if (periodeForVurdering.fom > rettighetsperiode.fom) {
        return Validation.Invalid(
            this,
            "Det er ikke tatt stilling til hele rettighetsperioden. Rettighetsperioden for saken starter ${rettighetsperiode.fom} mens vurderingens første periode starter ${periodeForVurdering.fom}. "
        )
    }

    if (periodeForVurdering.tom < rettighetsperiode.tom) {
        return Validation.Invalid(
            this,
            "Det er ikke tatt stilling til hele rettighetsperioden. Rettighetsperioden for saken slutter ${rettighetsperiode.tom} mens vurderingens siste periode slutter ${periodeForVurdering.tom}. "
        )
    }

    return Validation.Valid(this)
}