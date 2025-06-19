package no.nav.aap.behandlingsflyt.behandling.beregning.tidspunkt

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.komponenter.verdityper.Beløp
import java.time.LocalDate

data class BeregningYrkesskadeAvklaringResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val skalVurderes: List<YrkesskadeTilVurderingResponse>,
    val vurderinger: List<YrkesskadeBeløpVurderingResponse>
)

data class YrkesskadeTilVurderingResponse(val referanse: String, val skadeDato: LocalDate, val grunnbeløp: Beløp)

data class YrkesskadeBeløpVurderingResponse(
    val antattÅrligInntekt: Beløp,
    val referanse: String,
    val begrunnelse: String,
    val vurdertAv: VurdertAvResponse
)