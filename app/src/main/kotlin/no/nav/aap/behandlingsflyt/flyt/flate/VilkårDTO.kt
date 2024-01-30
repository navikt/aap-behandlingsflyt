package no.nav.aap.behandlingsflyt.flyt.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårtype

data class VilkårDTO(val vilkårtype: Vilkårtype, val perioder: List<VilkårsperiodeDTO>)
