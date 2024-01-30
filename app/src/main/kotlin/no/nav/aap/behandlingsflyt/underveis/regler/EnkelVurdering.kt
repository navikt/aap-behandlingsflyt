package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårtype

data class EnkelVurdering(val vilkår: Vilkårtype, val utfall: Utfall)
