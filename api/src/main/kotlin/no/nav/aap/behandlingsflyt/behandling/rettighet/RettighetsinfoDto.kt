package no.nav.aap.behandlingsflyt.behandling.rettighet

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import java.time.LocalDate

data class RettighetsinfoDto(
    val sisteDagMedRett: LocalDate?,
    val perioderMedRett: List<RettighetsperiodeDto>
)

data class RettighetsperiodeDto(
    val rettighetstype: RettighetsType,
    val fom: LocalDate,
    val tom: LocalDate
)