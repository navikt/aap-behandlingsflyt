package no.nav.aap.behandlingsflyt.behandling.kvalitetssikring

import no.nav.aap.behandlingsflyt.avklaringsbehov.flate.Historikk
import no.nav.aap.behandlingsflyt.behandling.totrinnsvurdering.TotrinnsVurderingResponse

data class KvalitetssikringGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val harGjortVilkårsvurderingerPåBehandling: Boolean,
    val vurderinger: List<TotrinnsVurderingResponse>,
    val historikk: List<Historikk>
)