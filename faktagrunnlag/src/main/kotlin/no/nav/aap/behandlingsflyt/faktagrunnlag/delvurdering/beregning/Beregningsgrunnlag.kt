package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Faktagrunnlag
import no.nav.aap.verdityper.GUnit

sealed interface Beregningsgrunnlag {
    fun grunnlaget(): GUnit
    fun faktagrunnlag(): Faktagrunnlag
    fun er6GBegrenset(): Boolean
    fun erGjennomsnitt(): Boolean
}
