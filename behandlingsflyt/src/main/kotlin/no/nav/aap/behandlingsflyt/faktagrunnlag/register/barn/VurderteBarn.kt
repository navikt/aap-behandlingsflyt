package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDateTime

data class VurderteBarn(
    val id: Long,
    val barn: List<VurdertBarn>,
    val vurdertAv: Bruker,
    val vurdertTidspunkt: LocalDateTime
)