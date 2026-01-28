package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway

import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class DagpengerRequest(
    val personIdent: String,
    val fraOgMedDato: String,
    val tilOgMedDato: String
)

class DagpengerResponse(
    val personIdent: String,
    val perioder: List<DagpengerPeriodeResponse>
)

class DagpengerPeriodeResponse(
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate,
    val kilde: DagpengerKilde,
    val ytelseType: DagpengerYtelseType
)

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