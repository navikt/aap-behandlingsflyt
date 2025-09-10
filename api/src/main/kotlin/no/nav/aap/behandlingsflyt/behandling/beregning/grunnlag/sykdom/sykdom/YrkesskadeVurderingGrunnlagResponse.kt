package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.InnhentetSykdomsOpplysninger
import java.time.LocalDate

data class YrkesskadeVurderingGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val opplysninger: InnhentetSykdomsOpplysninger,
    val yrkesskadeVurdering: YrkesskadevurderingResponse?
)

data class YrkesskadevurderingResponse(
    val begrunnelse: String,
    @Deprecated("Bruk relevanteYrkesskadeSaker")
    val relevanteSaker: List<String>,
    val relevanteYrkesskadeSaker: List<YrkesskadeSakResponse>,
    val andelAvNedsettelsen: Int?,
    val erÅrsakssammenheng: Boolean,
    val vurdertAv: VurdertAvResponse
)

data class YrkesskadeSakResponse(
    val referanse: String,
    val manuellYrkesskadeDato: LocalDate?
)