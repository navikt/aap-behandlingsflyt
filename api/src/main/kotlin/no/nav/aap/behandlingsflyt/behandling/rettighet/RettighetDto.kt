package no.nav.aap.behandlingsflyt.behandling.rettighet

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.PeriodeKvote
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import java.time.LocalDate

data class RettighetDto(
    val type: RettighetsType,
    val kvote: Int? = null,
    val bruktKvote: Int? = null,
    val gjenværendeKvote: Int? = null,
    val periodeKvoter: List<PeriodeKvote> = emptyList(),
    val startDato: LocalDate? = null,
    val maksDato: LocalDate? = null,
    val avslagDato: LocalDate? = null,
    val avslagÅrsak: Avslagstype? = null,
)
