package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering

data class BeregningTidspunktAvklaringDto(val vurdering: BeregningstidspunktVurdering?, val skalVurdereYtterligere: Boolean)