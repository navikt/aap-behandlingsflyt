package no.nav.aap.behandlingsflyt.behandling.beregning.tidspunkt

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurderingerMetaResponse
import java.time.LocalDate

data class BeregningTidspunktAvklaringResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurdering: BeregningstidspunktVurderingResponse?,
    val historiskeVurderinger: List<BeregningstidspunktVurderingResponse>,
    val skalVurdereYtterligere: Boolean
)

data class BeregningstidspunktVurderingResponse(
    val begrunnelse: String,
    val nedsattArbeidsevneDato: LocalDate,
    val ytterligereNedsattBegrunnelse: String?,
    val ytterligereNedsattArbeidsevneDato: LocalDate?,
    val vurderingerMeta: VurderingerMetaResponse,
)