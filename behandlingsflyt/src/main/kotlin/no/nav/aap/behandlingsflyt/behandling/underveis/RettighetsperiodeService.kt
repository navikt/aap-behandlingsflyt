package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.behandling.vilkår.overgangarbeid.OvergangArbeidVilkår
import no.nav.aap.behandlingsflyt.behandling.vilkår.overganguføre.OvergangUføreVilkår
import no.nav.aap.behandlingsflyt.behandling.vilkår.student.StudentVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

class RettighetsperiodeService {
    fun beregn(startDato: LocalDate): Rettighetsperioder {
        return Rettighetsperioder(
            arbeidssøkerPeriode = Periode(startDato, OvergangArbeidVilkår.utledVarighetSluttdato(startDato)),
            studentPeriode = Periode(startDato, StudentVilkår.utledVarighetSluttdato(startDato)),
            overgangUførePeriode = Periode(startDato, OvergangUføreVilkår.utledVarighetSluttdato(startDato)),
        )
    }

    fun utledMaksdatoForRettighet(rettighetstype: RettighetsType, startDato: LocalDate?): LocalDate? {
        if (startDato == null) {
            return null
        }
        return beregn(startDato).hentPeriodeForRettighetstype(rettighetstype)?.tom
    }
}
