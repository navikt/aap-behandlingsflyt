package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis

import no.nav.aap.komponenter.type.Periode

data class RettighetKvoter(
    val totalKvote: Int?,
    val bruktKvote: Int,
    val gjenværendeKvote: Int,
    val periodeKvoter: List<PeriodeKvote>
)

data class PeriodeKvote(
    val periode: Periode,
    val bruktKvote: Int,
    val gjenværendeKvote: Int?
)
