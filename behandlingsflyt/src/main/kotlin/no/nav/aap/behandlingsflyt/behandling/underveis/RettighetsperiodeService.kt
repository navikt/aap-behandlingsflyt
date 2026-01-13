package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

class RettighetsperiodeService {
    fun beregn(startDato: LocalDate): Rettighetsperioder {
        return Rettighetsperioder(
            studentPeriode = Periode(startDato, startDato.plusMonths(6)),
            arbeidssøkerPeriode = Periode(startDato, startDato.plusMonths(6)),
            overgangUførePeriode = Periode(startDato, startDato.plusMonths(8)),
        )
    }
}
