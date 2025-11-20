package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseGrunnlag
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent

data class SamordningYtelseVurderingGrunnlag(
    val ytelseGrunnlag: SamordningYtelseGrunnlag?,
    val vurderingGrunnlag: SamordningVurderingGrunnlag?
) : Faktagrunnlag

/**
 * Grunnlag fra smordningssteget som brukes i følgende steg.
 *
 * Alle fakta ligger i [SamordningYtelseVurderingGrunnlag] og
 * lagres ned som [no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag] sammen med denne.
 */
data class SamordningGrunnlag(
    val samordningPerioder: Set<SamordningPeriode>,
)

/**
 * En ferdig vurdert samordning-periode.
 */
data class SamordningPeriode(
    val periode: Periode,
    val gradering: Prosent
)
