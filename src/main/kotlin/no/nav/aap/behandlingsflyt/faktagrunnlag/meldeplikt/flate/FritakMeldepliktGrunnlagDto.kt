package no.nav.aap.behandlingsflyt.faktagrunnlag.meldeplikt.flate

import no.nav.aap.behandlingsflyt.avklaringsbehov.meldeplikt.Fritaksvurdering

data class FritakMeldepliktGrunnlagDto(val vurderinger: List<Fritaksvurdering>)
