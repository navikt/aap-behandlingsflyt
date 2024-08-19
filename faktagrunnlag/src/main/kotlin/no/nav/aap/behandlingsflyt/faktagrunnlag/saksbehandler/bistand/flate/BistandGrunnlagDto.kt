package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandVurdering

data class BistandGrunnlagDto(
    val vurdering: BistandVurdering?
)
