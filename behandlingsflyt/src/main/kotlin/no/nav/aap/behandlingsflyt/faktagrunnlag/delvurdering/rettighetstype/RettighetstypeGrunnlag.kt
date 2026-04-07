package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.komponenter.tidslinje.Tidslinje

data class RettighetstypeGrunnlag(
    val rettighetstypeTidslinje: Tidslinje<RettighetsType>
)