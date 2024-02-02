package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering

data class FritakMeldepliktGrunnlagDto(val vurderinger: List<Fritaksvurdering>)
