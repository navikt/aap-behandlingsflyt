package no.nav.aap.behandlingsflyt.behandling.flate

import no.nav.aap.behandlingsflyt.behandling.Status
import no.nav.aap.behandlingsflyt.flyt.flate.VilkårDTO
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import java.time.LocalDateTime
import java.util.*

data class DetaljertBehandlingDTO(
    val referanse: UUID,
    val type: String,
    val status: Status,
    val opprettet: LocalDateTime,
    val avklaringsbehov: List<AvklaringsbehovDTO>,
    val vilkår: List<VilkårDTO>,
    val aktivtSteg: StegType,
    val versjon: Long
)
