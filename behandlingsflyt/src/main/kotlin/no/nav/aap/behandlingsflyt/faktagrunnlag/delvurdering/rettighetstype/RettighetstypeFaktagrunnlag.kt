package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype

import no.nav.aap.behandlingsflyt.behandling.underveis.Kvoter
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår

class RettighetstypeFaktagrunnlag(
    val vilkår: List<Vilkår>,
    val kvoter: Kvoter,
): Faktagrunnlag