package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate.Historikk
import no.nav.aap.behandlingsflyt.behandling.totrinnsvurdering.TotrinnsVurderingResponse
import java.time.LocalDateTime

data class FatteVedtakGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val harGjortVilkårsvurderingerPåBehandling: Boolean,
    val vurderinger: List<TotrinnsVurderingResponse>,
    val historikk: List<Historikk>,
    val besluttetAv: BeslutterDto?
)

data class BeslutterDto(
    val ident: String,
    val navn: String,
    val kontor: String,
    val tidspunkt: LocalDateTime,
)