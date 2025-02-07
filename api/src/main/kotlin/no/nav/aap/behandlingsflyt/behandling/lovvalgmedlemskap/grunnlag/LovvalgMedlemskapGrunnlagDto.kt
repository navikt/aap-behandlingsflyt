package no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode

data class LovvalgMedlemskapGrunnlagDto (
    val vilkårsperioder: List<Vilkårsperiode>
)