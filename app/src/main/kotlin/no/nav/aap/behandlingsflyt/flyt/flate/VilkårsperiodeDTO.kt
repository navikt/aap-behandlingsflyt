package no.nav.aap.behandlingsflyt.flyt.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Utfall
import no.nav.aap.verdityper.Periode

data class VilkårsperiodeDTO(
    val periode: Periode,
    val utfall: Utfall,
    val manuellVurdering: Boolean,
    val begrunnelse: String?,
    val avslagsårsak: Avslagsårsak?,
    val innvilgelsesårsak: Innvilgelsesårsak?
)
