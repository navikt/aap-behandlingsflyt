package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway

import no.nav.aap.komponenter.type.Periode

data class TiltakspengerPeriode(
    val periode: Periode,
    val kilde: DagpengerKilde,
    val tiltakspengerYtelseType: TiltakspengerYtelseType
)

enum class TiltakspengerYtelseType{
    TILTAKSPENGER, TILTAKSPENGER_OG_BARNETILLEGG, INGENTING
}
enum class TiltakspengerKilde{
    TPSAK, ARENA
}