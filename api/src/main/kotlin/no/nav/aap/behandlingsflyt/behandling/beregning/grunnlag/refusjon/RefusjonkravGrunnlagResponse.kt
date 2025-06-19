package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.refusjon

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import java.time.LocalDate


data class RefusjonkravGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val gjeldendeVurderinger: List<RefusjonkravVurderingResponse>?,
    val historiskeVurderinger: List<RefusjonkravVurderingResponse>?
)

data class RefusjonkravVurderingResponse(
    val harKrav: Boolean,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val vurdertAv: VurdertAvResponse
)