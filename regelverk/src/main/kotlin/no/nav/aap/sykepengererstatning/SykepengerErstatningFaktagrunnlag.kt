package no.nav.aap.sykepengererstatning

import no.nav.aap.misc.Faktagrunnlag
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.sykdom.SykdomGrunnlag

class SykepengerErstatningFaktagrunnlag(
    val rettighetsperiode: Periode,
    val sykepengeerstatningGrunnlag: SykepengerErstatningGrunnlag?,
    val sykdomGrunnlag: SykdomGrunnlag?,
) : Faktagrunnlag