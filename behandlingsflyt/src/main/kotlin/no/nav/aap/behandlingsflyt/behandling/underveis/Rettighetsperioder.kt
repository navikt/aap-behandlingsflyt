package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.komponenter.type.Periode

class Rettighetsperioder (
    val arbeidssøkerPeriode: Periode,
    val studentPeriode: Periode,
    val overgangUførePeriode: Periode
) {
    fun hentPeriodeForRettighetstype(type: RettighetsType): Periode? {
        return when (type) {
            RettighetsType.ARBEIDSSØKER -> this.arbeidssøkerPeriode
            RettighetsType.STUDENT -> this.studentPeriode
            RettighetsType.VURDERES_FOR_UFØRETRYGD -> this.overgangUførePeriode
            else -> null
        }
    }
}
