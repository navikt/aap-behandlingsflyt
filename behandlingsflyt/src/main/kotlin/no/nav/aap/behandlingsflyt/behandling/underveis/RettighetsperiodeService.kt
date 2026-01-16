package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

class RettighetsperiodeService {
    fun beregn(startDato: LocalDate): Rettighetsperioder {
        return Rettighetsperioder(
            arbeidssøkerPeriode = Periode(startDato, startDato.plusMonths(6)),
            studentPeriode = Periode(startDato, startDato.plusMonths(6)),
            overgangUførePeriode = Periode(startDato, startDato.plusMonths(8)),
        )
    }

    fun utledMaksdatoForRettighet(rettighetstype: RettighetsType, startdato: LocalDate): LocalDate? {
        return beregn(startdato).hentPeriodeForRettighetstype(rettighetstype)?.tom
    }
}
