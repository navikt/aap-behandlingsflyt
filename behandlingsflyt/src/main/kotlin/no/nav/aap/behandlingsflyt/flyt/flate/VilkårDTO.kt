package no.nav.aap.behandlingsflyt.flyt.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype

data class VilkårDTO(val vilkårtype: Vilkårtype, val perioder: List<VilkårsperiodeDTO>)
