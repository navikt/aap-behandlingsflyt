package no.nav.aap.overgangarbeid

import no.nav.aap.misc.Faktagrunnlag
import no.nav.aap.komponenter.type.Periode

data class OvergangArbeidFaktagrunnlag(
    val rettighetsperiode: Periode,
    val overgangArbeidGrunnlag: OvergangArbeidGrunnlag,
) : Faktagrunnlag