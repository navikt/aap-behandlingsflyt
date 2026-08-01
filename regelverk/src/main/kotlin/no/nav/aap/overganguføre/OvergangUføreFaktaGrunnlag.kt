package no.nav.aap.overganguføre

import no.nav.aap.misc.Faktagrunnlag
import no.nav.aap.komponenter.type.Periode

data class OvergangUføreFaktagrunnlag(
    val rettighetsperiode: Periode,
    val overgangUføreGrunnlag: OvergangUføreGrunnlag?
) : Faktagrunnlag