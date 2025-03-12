package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import java.time.LocalDate

data class VurderingerForSamordning(
    val begrunnelse: String,
    val maksDatoEndelig: Boolean,
    val maksDato: LocalDate?,
    val vurderteSamordninger: List<SamordningVurdering>
)