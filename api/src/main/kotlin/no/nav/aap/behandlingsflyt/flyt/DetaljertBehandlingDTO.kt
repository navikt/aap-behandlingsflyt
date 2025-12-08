package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class DetaljertBehandlingDTO(
    val referanse: UUID,
    val type: TypeBehandling,
    val status: Status,
    val opprettet: LocalDateTime,
    val avklaringsbehov: List<AvklaringsbehovDTO>,
    val vilkår: List<VilkårDTO>,
    val aktivtSteg: StegType,
    val skalForberede: Boolean,
    val versjon: Long,
    val virkningstidspunkt: LocalDate?,
    val vedtaksdato: LocalDate?,
    val kravMottatt: LocalDate?,
    val tilhørendeKlagebehandling: UUID?,
    val vurderingsbehovOgÅrsaker: List<VurderingsbehovOgÅrsak>,
    val arenaStatus: ArenaStatusDTO?
)
