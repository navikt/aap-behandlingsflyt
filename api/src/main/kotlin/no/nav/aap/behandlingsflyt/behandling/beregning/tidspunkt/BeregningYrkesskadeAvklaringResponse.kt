package no.nav.aap.behandlingsflyt.behandling.beregning.tidspunkt

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurderingerMetaResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.SkadekombinasjonRegister
import no.nav.aap.komponenter.verdityper.Beløp
import java.time.LocalDate

data class BeregningYrkesskadeAvklaringResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val skalVurderes: List<YrkesskadeTilVurderingResponse>,
    val vurderinger: List<YrkesskadeBeløpVurderingResponse>,
    val historiskeVurderinger: List<YrkesskadeBeløpVurderingResponse>
)

data class YrkesskadeTilVurderingResponse(
    val referanse: String,
    val saksnummer: Int?,
    val kilde: String,
    val skadeDato: LocalDate,
    val vedtaksdato: LocalDate? = null,
    val skadeart: String? = null,
    val diagnose: String? = null,
    val skadekombinasjoner: List<SkadekombinasjonRegister>? = null,
    val skadekombinasjonerTekst: String? = null,
    val grunnbeløp: Beløp
)

data class YrkesskadeBeløpVurderingResponse(
    val antattÅrligInntekt: Beløp,
    val referanse: String,
    val begrunnelse: String,
    val vurderingerMeta: VurderingerMetaResponse,
)