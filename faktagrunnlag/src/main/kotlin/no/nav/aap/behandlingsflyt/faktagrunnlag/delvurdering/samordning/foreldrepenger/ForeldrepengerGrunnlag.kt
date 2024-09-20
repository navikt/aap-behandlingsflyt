package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.foreldrepenger

data class ForeldrepengerGrunnlag (
    val perioderId: Long,
    val perioder: List<ForeldrepengerPeriode>
)