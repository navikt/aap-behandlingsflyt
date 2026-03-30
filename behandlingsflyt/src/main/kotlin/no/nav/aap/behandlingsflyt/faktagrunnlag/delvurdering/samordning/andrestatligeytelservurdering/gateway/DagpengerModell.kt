package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway

import no.nav.aap.komponenter.type.Periode

data class DagpengerPeriode(
    val periode: Periode,
    val kilde: DagpengerKilde,
    val dagpengerYtelseType: DagpengerYtelseType
)

enum class DagpengerYtelseType{
    DAGPENGER_ARBEIDSSOKER_ORDINAER, DAGPENGER_PERMITTERING_ORDINAER, DAGPENGER_PERMITTERING_FISKEINDUSTRI
}
enum class DagpengerKilde{
    ARENA, DP_SAK
}