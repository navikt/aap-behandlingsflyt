package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.InnhentetSykdomsOpplysninger

data class YrkesskadeVurderingGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val opplysninger: InnhentetSykdomsOpplysninger,
    val yrkesskadeVurdering: YrkesskadevurderingResponse?
)

data class YrkesskadevurderingResponse(
    val begrunnelse: String,
    val relevanteSaker: List<String>,
    val andelAvNedsettelsen: Int?,
    val erÅrsakssammenheng: Boolean,
    val vurdertAv: VurdertAvResponse
)