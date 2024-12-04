package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning

data class SamordningGrunnlag (
    val id: Long,
    val samordningPerioder: List<SamordningPeriode>
)