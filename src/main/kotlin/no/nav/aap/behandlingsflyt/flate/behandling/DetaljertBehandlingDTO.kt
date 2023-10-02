package no.nav.aap.behandlingsflyt.flate.behandling

import no.nav.aap.behandlingsflyt.domene.behandling.Status
import no.nav.aap.flyt.StegType
import java.time.LocalDateTime
import java.util.*

data class DetaljertBehandlingDTO(
    val referanse: UUID,
    val type: String,
    val status: Status,
    val opprettet: LocalDateTime,
    val avklaringsbehov: List<no.nav.aap.behandlingsflyt.flate.behandling.AvklaringsbehovDTO>,
    val vilkår: List<VilkårDTO>,
    val aktivtSteg: StegType
)
