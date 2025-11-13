package no.nav.aap.behandlingsflyt.behandling.arbeidsopptrapping

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.LocalDateTime

data class ArbeidsopptrappingGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurderinger: List<ArbeidsopptrappingVurderingResponse>?,
    val gjeldendeVedtatteVurderinger: List<ArbeidsopptrappingVurderingResponse>?,
    val historikk: Set<ArbeidsopptrappingVurderingResponse>?,
    val perioderSomKanVurderes: List<Periode>?
)

data class ArbeidsopptrappingVurderingResponse(
    val begrunnelse: String,
    val vurderingsTidspunkt: LocalDateTime,
    val reellMulighetTilOpptrapping: Boolean,
    val rettPaaAAPIOpptrapping: Boolean,
    val fraDato: LocalDate,
    val vurdertAv: VurdertAvResponse
)