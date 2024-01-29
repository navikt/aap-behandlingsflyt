package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.verdityper.GUnit
import no.nav.aap.behandlingsflyt.vilkår.Faktagrunnlag

interface Beregningsgrunnlag {
    fun grunnlaget(): GUnit
    fun faktagrunnlag(): Faktagrunnlag
}
