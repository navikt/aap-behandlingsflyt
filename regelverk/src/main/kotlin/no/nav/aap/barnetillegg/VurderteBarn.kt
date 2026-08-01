package no.nav.aap.barnetillegg

import java.time.LocalDateTime
import no.nav.aap.komponenter.verdityper.Bruker

data class VurderteBarn(
    val id: Long,
    val barn: List<VurdertBarn>,
    val vurdertAv: Bruker,
    val vurdertTidspunkt: LocalDateTime
)