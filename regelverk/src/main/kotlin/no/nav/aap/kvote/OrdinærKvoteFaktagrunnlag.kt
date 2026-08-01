package no.nav.aap.kvote

import no.nav.aap.rettighetstype.KvoteVurdering
import no.nav.aap.misc.Faktagrunnlag
import no.nav.aap.komponenter.tidslinje.Tidslinje

data class OrdinærKvoteFaktagrunnlag(
    val kvotevurdering: Tidslinje<KvoteVurdering>,
    val kvoter: Kvoter
) : Faktagrunnlag