package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.komponenter.type.Periode

class Rettighetsperioder (
    val studentPeriode: Periode,
    val arbeidssøkerPeriode: Periode,
    val overgangUførePeriode: Periode
) {
    fun hentPeriodeForRettighetstype(type: RettighetsType): Periode? {
        return when (type) {
            RettighetsType.STUDENT -> this.studentPeriode
            RettighetsType.ARBEIDSSØKER -> this.arbeidssøkerPeriode
            RettighetsType.VURDERES_FOR_UFØRETRYGD -> this.overgangUførePeriode
            else -> null
        }
    }
}
