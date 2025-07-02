package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import java.time.LocalDate

data class VilkårDTO(
    val vilkårtype: Vilkårtype,
    val perioder: List<VilkårsperiodeDTO>,
    val vurdertDato: LocalDate?
)