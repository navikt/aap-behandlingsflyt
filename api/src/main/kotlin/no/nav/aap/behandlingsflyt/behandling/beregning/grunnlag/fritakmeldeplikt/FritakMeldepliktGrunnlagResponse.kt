package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.fritakmeldeplikt

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import java.time.LocalDate
import java.time.LocalDateTime

data class FritakMeldepliktGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val historikk: Set<FritakMeldepliktVurderingResponse>,
    val gjeldendeVedtatteVurderinger: List<FritakMeldepliktVurderingResponse>,
    val vurderinger: List<FritakMeldepliktVurderingResponse>,
)

data class FritakMeldepliktVurderingResponse(
    val begrunnelse: String,
    val vurderingsTidspunkt: LocalDateTime,
    val harFritak: Boolean,
    val fraDato: LocalDate,
    val vurdertAv: VurdertAvResponse
)