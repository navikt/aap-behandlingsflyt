package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering

data class BistandGrunnlagDto(
    val vurdering: BistandVurderingDto?,
    val gjeldendeVedtatteVurderinger: List<BistandVurderingDto>,
    val historiskeVurderinger: List<BistandVurderingDto>,
    val gjeldendeSykdsomsvurderinger: List<Sykdomsvurdering>
)
