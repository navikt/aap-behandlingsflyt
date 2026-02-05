package no.nav.aap.behandlingsflyt.behandling.vilkår.kvote

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KvoteVurdering
import no.nav.aap.behandlingsflyt.behandling.underveis.Kvoter
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.komponenter.tidslinje.Tidslinje

data class OrdinærKvoteFaktagrunnlag(
    val kvotevurdering: Tidslinje<KvoteVurdering>,
    val kvoter: Kvoter
) : Faktagrunnlag