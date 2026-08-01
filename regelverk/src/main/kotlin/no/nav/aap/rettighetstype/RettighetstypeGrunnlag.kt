package no.nav.aap.rettighetstype

import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.vilkårsresultat.RettighetsType

data class RettighetstypeGrunnlag(
    val rettighetstypeTidslinje: Tidslinje<RettighetsType>
)