package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.vilkårsresultat.Avslagsårsak
import no.nav.aap.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.vilkårsresultat.Utfall
import no.nav.aap.komponenter.type.Periode

data class VilkårsperiodeDTO(
    val periode: Periode,
    val utfall: Utfall,
    val manuellVurdering: Boolean,
    val begrunnelse: String?,
    val avslagsårsak: Avslagsårsak?,
    val innvilgelsesårsak: Innvilgelsesårsak?
)
