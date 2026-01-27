package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway

import java.time.LocalDate

class DagpengerResponse(
    val personIdent: String,
    val perioder: List<DagpengerPeriode>
)

class DagpengerPeriode(
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate,
    val kilde: DagpengerKilde,
    val ytelseType: DagpengerYtelseType
)

enum class DagpengerYtelseType{
    DAGPENGER_ARBEIDSSOKER_ORDINAER, DAGPENGER_PERMITTERING_ORDINAER, DAGPENGER_PERMITTERING_FISKEINDUSTRI
}
enum class DagpengerKilde{
    ARENA, DP_SAK
}