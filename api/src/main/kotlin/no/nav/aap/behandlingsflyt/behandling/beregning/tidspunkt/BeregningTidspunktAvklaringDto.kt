package no.nav.aap.behandlingsflyt.behandling.beregning.tidspunkt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering

data class BeregningTidspunktAvklaringDto(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurdering: BeregningstidspunktVurdering?,
    val skalVurdereYtterligere: Boolean
)