package no.nav.aap.behandlingsflyt.flyt.flate

import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
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
