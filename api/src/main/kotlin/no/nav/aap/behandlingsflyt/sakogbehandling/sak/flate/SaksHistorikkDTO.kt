package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDateTime

data class SaksHistorikkDTO (
    val behandlinger: List<BehandlingHistorikkDTO>
)

data class BehandlingHistorikkDTO(
    val hendelser: List<BehandlingHendelseDTO> = emptyList()
)

data class BehandlingHendelseDTO(
    val hendelse: BehandlingHendelseType,
    val tidspunkt: LocalDateTime,
    val utførtAv: String? = null,
    val årsaker: List<String>? = emptyList(),
    val begrunnelse: String? = null,
    val resultat: String? = null,
)

enum class BehandlingHendelseType {
    SATT_PÅ_VENT, //  Behandling satt på vent, med årsak og begrunnelse
    TATT_AV_VENT, // Behandling tatt av vent, med årsak
    VEDTAK_FATTET, // Resultat, evt avslag med årsak
    BREV_SENDT, //  Sendte brev med tittel
    SENDT_TIL_BESLUTTER,
    RETUR_FRA_BESLUTTER,
    SENDT_TIL_KVALITETSSIKRER, // med resultat
    RETUR_FRA_KVALITETSSIKRER, // med resultat og eventuell årsak for retur + begrunnelse
    REVURDERING_OPPRETTET,
    FØRSTEGANGSBEHANDLING_OPPRETTET,
    KLAGE_OPPRETTET,
    MOTTATT_DIALOGMELDING, // ? Mottatt legeerklæring og dialogmelding
    BESTILT_LEGEERKLÆRING // ? Mottatt legeerklæring og dialogmelding
    //  Behandling / vurderingsbehov startet, med årsak, vurderingsbehov og eventuell begrunnelse
}