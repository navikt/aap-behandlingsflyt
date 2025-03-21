package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingDto

data class BistandGrunnlagDto(
    val vurdering: BistandVurderingDto?,
    val gjeldendeVedtatteVurderinger: List<BistandVurderingDto>,
    val historiskeVurderinger: List<BistandVurderingDto>,
    val gjeldendeSykdsomsvurderinger: List<SykdomsvurderingDto>
)
