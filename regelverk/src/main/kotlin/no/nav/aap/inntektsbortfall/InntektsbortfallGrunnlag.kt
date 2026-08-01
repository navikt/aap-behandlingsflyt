package no.nav.aap.inntektsbortfall

import no.nav.aap.misc.Faktagrunnlag
import no.nav.aap.beregning.InntektsbortfallVurdering

data class InntektsbortfallGrunnlag(
    val inntektsbortfallKanBehandlesAutomatisk: InntektsbortfallKanBehandlesAutomatisk,
    val manuellVurdering: InntektsbortfallVurdering?
) : Faktagrunnlag