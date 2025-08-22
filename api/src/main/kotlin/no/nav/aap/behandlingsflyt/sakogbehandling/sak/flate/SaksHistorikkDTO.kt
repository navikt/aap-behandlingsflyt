package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.ÅrsakTilRetur
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import java.time.LocalDateTime

data class BehandlingHistorikkDTO(
    val hendelser: List<BehandlingHendelseDTO> = emptyList()
)

data class BehandlingHendelseDTO(
    val hendelse: BehandlingHendelseType,
    val tidspunkt: LocalDateTime,
    val utførtAv: String? = null,
    val årsakerTilRetur: List<ÅrsakTilRetur>? = emptyList(),
    val årsakTilSattPåVent: ÅrsakTilSettPåVent? = null,
    val årsakerTilOpprettelse: List<Vurderingsbehov?> = emptyList(),
    val begrunnelse: String? = null,
    val resultat: String? = null,
)

enum class BehandlingHendelseType {
    SATT_PÅ_VENT,
    TATT_AV_VENT,
    VEDTAK_FATTET,
    BREV_SENDT,
    SENDT_TIL_BESLUTTER,
    RETUR_FRA_BESLUTTER,
    SENDT_TIL_KVALITETSSIKRER,
    RETUR_FRA_KVALITETSSIKRER,
    REVURDERING_OPPRETTET,
    FØRSTEGANGSBEHANDLING_OPPRETTET,
    KLAGE_OPPRETTET,
    MOTTATT_DIALOGMELDING,
    BESTILT_LEGEERKLÆRING
}