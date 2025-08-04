package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import java.time.LocalDate
import java.time.LocalDateTime

data class MeldepliktRimeligGrunnGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val historikk: Set<MeldepliktRimeligGrunnVurderingResponse>,
    val gjeldendeVedtatteVurderinger: List<MeldepliktRimeligGrunnVurderingResponse>,
    val vurderinger: List<MeldepliktRimeligGrunnVurderingResponse>,
)

data class MeldepliktRimeligGrunnVurderingResponse(
    val begrunnelse: String,
    val vurderingsTidspunkt: LocalDateTime,
    val harRimeligGrunn: Boolean,
    val fraDato: LocalDate,
    val vurdertAv: VurdertAvResponse
)