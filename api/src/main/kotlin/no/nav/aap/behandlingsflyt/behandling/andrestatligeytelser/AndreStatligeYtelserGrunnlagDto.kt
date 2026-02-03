package no.nav.aap.behandlingsflyt.behandling.andrestatligeytelser

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerKilde
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerYtelseType
import java.time.LocalDate

data class AndreStatligeYtelserGrunnlagDto (
    val dagpengerPerioder : List <DagpengerPeriodeDto> = emptyList()
)

data class DagpengerPeriodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val kilde: DagpengerKilde,
    val dagpengerYtelseType: DagpengerYtelseType
)
