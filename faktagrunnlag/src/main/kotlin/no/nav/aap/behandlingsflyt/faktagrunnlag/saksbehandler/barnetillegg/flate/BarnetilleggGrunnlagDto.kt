package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg.flate

data class BarnetilleggGrunnlagDto(
    val manueltOppgitteBarn: List<ManueltBarnDto>,
    val folkeregistrerteBarn: List<FolkeregistrertBarnDto>,
    val vurdering: ManuelleBarnVurderingDto?
)