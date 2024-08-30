package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning

// TODO: Vet ikke hva som skal inn her enda
data class SamordningGrunnlagDto(val samordningGraderingVurdering: SamordningGraderingVurderingDto)
data class SamordningGraderingVurderingDto(val begrunnelse: String, val ytelsesVurderinger: List<YtelsesVurderingDto>)
data class YtelsesVurderingDto(val ytelse: String, val gradering: Int)