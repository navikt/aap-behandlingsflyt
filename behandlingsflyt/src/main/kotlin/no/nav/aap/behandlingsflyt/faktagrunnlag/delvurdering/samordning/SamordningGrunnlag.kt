package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent

/**
 * Grunnlag fra smordningssteget som brukes i følgende steg.
 *
 * Alle fakta ligger i [no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingGrunnlag] og
 *  hentes i [no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingRepository].
 */
data class SamordningGrunnlag( // ->resultat, ta med referanse til rådataen
    val id: Long,
    val samordningPerioder: List<SamordningPeriode>,
)

// se hvordan faktagrunnlag er gjort

/**
 * En ferdig vurdert samordning-periode.
 */
data class SamordningPeriode(
    val periode: Periode,
    val gradering: Prosent
)