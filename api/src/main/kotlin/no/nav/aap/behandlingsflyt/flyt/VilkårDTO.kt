package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.vilkårsresultat.Vilkårtype
import java.time.LocalDate

data class VilkårDTO(
    val vilkårtype: Vilkårtype,
    val perioder: List<VilkårsperiodeDTO>,
    val vurdertDato: LocalDate?
)