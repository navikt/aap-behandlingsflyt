package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway

import java.time.LocalDate

data class TiltakspengerPeriode(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val kilde: TiltakspengerKilde,
    val tiltakspengerYtelseType: TiltakspengerYtelseType
)

enum class TiltakspengerYtelseType{
    TILTAKSPENGER, TILTAKSPENGER_OG_BARNETILLEGG, INGENTING
}

enum class TiltakspengerKilde{
    TPSAK, ARENA
}