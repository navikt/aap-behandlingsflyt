package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.sykepenger

data class SykepengerGrunnlag (
    val perioderId: Long,
    val perioder: List<SykepengerPeriode>
)